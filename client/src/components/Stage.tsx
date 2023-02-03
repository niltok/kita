import React, {useEffect, useState} from "react"
import {useAppSelector} from "../storeHook"
import {keyEvents$, sendSocket$, seqDrawables$} from "../dbus"
import {Drawable, matchI} from "../types/Drawable"
import * as pixi from 'pixi.js'
import {getKeyCode, useKeyboard, useSubscribe, useWindowSize} from "../utils"
import {store} from "../store"

export type SeqDrawable = { operate: 'set' | 'diff', seq: number, data: { [key: string]: Drawable } };
type SeqState = {
    seq: number
    hashDrawable: Map<string, Drawable>
    assets: any
}

function renderDrawable(drawable: Drawable, assets: any, cache?: Map<string, pixi.DisplayObject>) {
    const setCommonProp = (display: pixi.DisplayObject) => {
        display.x = drawable.x
        display.y = drawable.y
        display.zIndex = drawable.zIndex
        display.rotation = drawable.angle
        display.pivot.set(0.5, 0.5)
        if (display instanceof pixi.Sprite) display.anchor.set(.5, .5)
    }
    const cached = cache?.get(drawable.key)
    return matchI(drawable) ({
        Drawable$Sprite(sprite) {
            const display = cached instanceof pixi.Sprite && cached ||
                pixi.Sprite.from(assets[sprite.bundle][sprite.asset])
            setCommonProp(display)
            return display
        },
        Drawable$Text(text) {
            const display = cached instanceof pixi.Text && cached ||
                new pixi.Text(text.text, {
                    fontFamily: "Sourcehanserifcn Vf.ttf",
                    ...text.style
                })
            setCommonProp(display)
            return display
        },
        Drawable$Container(container) {
            const display = cached instanceof pixi.Container && cached ||
                new pixi.Container()
            display.removeChildren()
            container.children.forEach(d => display.addChild(renderDrawable(d, assets)))
            display.sortableChildren = true
            display.sortChildren()
            setCommonProp(display)
            return display
        },
        Drawable$AnimatedSprite(animated) {
            const display = cached instanceof pixi.AnimatedSprite && cached ||
                new pixi.AnimatedSprite(assets[animated.bundle][animated.asset].animations[animated.animation])
            if (animated.playing) display.gotoAndPlay(animated.initialFrame)
            else display.gotoAndStop(animated.initialFrame)
            setCommonProp(display)
            return display
        }
    })
}

function diffDrawables(camera: pixi.Container, state: SeqState, val: SeqDrawable) {
    if (state.seq >= val.seq) return // equiv this.seq + 1 > diff.seq
    if (state.seq + 1 < val.seq) {
        state.seq = 2 << 31
        sendSocket$.next({ type: "state.seq.require", target: "starDrawables" })
        return
    }
    // this.seq + 1 == diff.seq
    state.seq = val.seq
    for (const k in val.data) {
        if (state.hashDrawable.has(k)) {
            if (val.data[k] === null)
                state.hashDrawable.delete(k)
            else console.error("seq-diff: duplicate drawables")
            continue
        }
        state.hashDrawable.set(k, val.data[k])
    }
}

function setDrawables(camera: pixi.Container, state: SeqState, val: SeqDrawable) {
    state.hashDrawable.clear()
    for (const key in val.data) {
        state.hashDrawable.set(key, val.data[key])
    }
    state.seq = val.seq
}

function useAutoRequire() {
    const state = useAppSelector(state => state.gameState.connection.state)
    useEffect(() => {
        if (state == 'connected') {
            sendSocket$.next({type: "state.seq.require", target: "starDrawables"})
        }
    }, [state])
}

const keyMapper = {
    "KeyW": {action: "up", value: 2},
    "KeyW!": "up",
    "KeyS": {action: "down", value: 2},
    "KeyS!": "down",
    "KeyA": {action: "left", value: 2},
    "KeyA!": "left",
    "KeyD": {action: "right", value: 2},
    "KeyD!": "right",
}

function handleKeyEvent(e: KeyboardEvent, mapper: { [key: string]: string | {action: string, value: number} }) {
    const action = mapper[getKeyCode(e)]
    if (typeof action == 'undefined') return
    if (typeof action == 'string')
        sendSocket$.next({
            type: "star.operate.key",
            action
        })
    else if (typeof action == 'object')
        sendSocket$.next({
            type: "star.operate.key",
            ...action
        })
}

export const Stage = () => {
    useKeyboard();
    useWindowSize();
    useSubscribe(keyEvents$, e => handleKeyEvent(e, keyMapper))
    const [canvas, setCanvas] = useState<HTMLCanvasElement | null>(null)
    const assets = useAppSelector(state => state.gameState.assets)
    useAutoRequire();
    useEffect(() => {
        if (canvas == null) return
        const app = new pixi.Application({
            resizeTo: window,
            view: canvas,
            antialias: true,
        })
        const camera = new pixi.Container()
        camera.sortableChildren = true
        app.stage.addChild(camera)
        const state: SeqState = {
            seq: 0,
            assets,
            hashDrawable: new Map<string, Drawable>()
        }
        const seqSub = seqDrawables$.subscribe({
            next(val) {
                if (val.operate == 'set') setDrawables(camera, state, val)
                else diffDrawables(camera, state, val)
                camera.removeChildren()
                state.hashDrawable.forEach(drawable => {
                    camera.addChild(renderDrawable(drawable, assets))
                })
                camera.sortChildren()
            }
        })
        const storeUnsub = store.subscribe(() => {
            const { star, windowSize } = store.getState().gameState
            camera.x = star.camera.x + windowSize.width / 2
            camera.y = star.camera.y + windowSize.height / 2
            camera.rotation = star.camera.rotation
        })
        return () => {
            storeUnsub()
            seqSub.unsubscribe()
            app.destroy()
        }
    }, [canvas])
    console.log("rendered at ", Date.now())
    return <canvas ref={setCanvas}></canvas>
}
