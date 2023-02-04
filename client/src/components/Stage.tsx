import React, {useEffect, useState} from "react"
import {useAppSelector} from "../storeHook"
import {keyEvents$, sendSocket$, seqDrawables$} from "../dbus"
import {Drawable, matchI} from "../types/Drawable"
import * as pixi from 'pixi.js'
import {applyObjectDiff, getKeyCode, useKeyboard, useSubscribe, useWindowSize} from "../utils"
import {store} from "../store"

export type SeqDrawable = { data: { [key: string]: Drawable } };
type SeqState = {
    drawables: { [key: string]: Drawable }
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
            assets,
            drawables: {}
        }
        const seqSub = seqDrawables$.subscribe({
            next(val) {
                applyObjectDiff(state.drawables, val.data)
                camera.removeChildren()
                for (const key in state.drawables) {
                    const drawable = state.drawables[key]
                    drawable.key = key
                    camera.addChild(renderDrawable(drawable, assets))
                }
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
