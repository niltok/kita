import React, {useEffect, useState} from "react"
import {useAppSelector} from "../storeHook"
import {keyEvents$, sendSocket$, seqDrawables$} from "../dbus"
import {Drawable, matchI} from "../types/Drawable"
import * as pixi from 'pixi.js'
import {applyObjectDiff, FPS, getKeyCode} from "../utils/common"
import {store} from "../store"
import Preprocessor from '../preprocessor.worker?worker'
import {useKeyboard, useSubscribe, useWindowSize} from "../utils/react";
import {DisplayObject} from "pixi.js";
import {ModifyEvent} from "../types/Preprocessor";

export type SeqDrawable = { data: { [key: string]: Drawable } };
type SeqState = {
    drawables: Map<string, Drawable>
    cache: Map<string, pixi.DisplayObject>
    assets: any
}

function renderDrawable(drawable: Drawable, assets: any, cache?: DisplayObject) {
    const setCommonProp = (display: pixi.DisplayObject) => {
        display.x = drawable.x
        display.y = drawable.y
        display.zIndex = drawable.zIndex
        display.rotation = drawable.angle
        display.pivot.set(0.5, 0.5)
        if (display instanceof pixi.Sprite) display.anchor.set(.5, .5)
    }
    return matchI(drawable) ({
        Sprite(sprite) {
            const display = cache as pixi.Sprite || new pixi.Sprite()
            display.texture = assets[sprite.bundle][sprite.asset]
            setCommonProp(display)
            return display
        },
        Text(text) {
            const display = cache as pixi.Text || new pixi.Text()
            display.text = text.text
            display.style = {
                fontFamily: ["Sourcehanserifcn Vf.ttf"],
                ...text.style
            }
            setCommonProp(display)
            return display
        },
        Container(container) {
            const display = cache as pixi.Container || new pixi.Container()
            display.removeChildren()
            container.children.forEach(d => display.addChild(renderDrawable(d, assets)))
            display.sortableChildren = true
            display.sortChildren()
            display.sortableChildren = false
            setCommonProp(display)
            return display
        },
        AnimatedSprite(animated) {
            const display = cache as pixi.AnimatedSprite || new pixi.AnimatedSprite(
                assets[animated.bundle][animated.asset].animations[animated.animation])
            display.textures = assets[animated.bundle][animated.asset].animations[animated.animation]
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
            antialias: false,
            autoDensity: true,
        })
        app.ticker.maxFPS = FPS
        const camera = new pixi.Container()
        app.stage.addChild(camera)
        const preprocessor = new Preprocessor()
        const state: SeqState = {
            assets,
            drawables: new Map<string, Drawable>(),
            cache: new Map<string, pixi.DisplayObject>
        }
        const seqSub = seqDrawables$.subscribe({
            next(val) {
                for (const k in val.data) {
                    const d = state.drawables.get(k), vd = val.data[k]
                    if (vd === null) state.drawables.delete(k)
                    else if (d === undefined) state.drawables.set(k, vd)
                    else applyObjectDiff(d, vd)
                    const cache = state.cache.get(k)
                    if (cache) renderDrawable(vd, assets, cache)
                }
                preprocessor.postMessage({
                    drawables: val.data
                })
            }
        })
        preprocessor.onmessage = (e: MessageEvent<ModifyEvent>) => {
            e.data.add.forEach((key) => {
                try {
                    const cache = state.cache.get(key)
                    const display = renderDrawable(state.drawables.get(key)!, assets, cache)
                    if (!cache) {
                        camera.addChild(display)
                        state.cache.set(key, display)
                    }
                } catch (e) {
                    console.error(key, e)
                }
            })
            e.data.delete.forEach((key) => {
                camera.removeChild(state.cache.get(key)!)
                state.cache.delete(key)
            })
            camera.sortableChildren = true
            camera.sortChildren()
            camera.sortableChildren = false
        }
        const cache: any = {
            camera: null,
            windowSize: null
        }
        const storeUnsub = store.subscribe(() => {
            const { star, windowSize } = store.getState().gameState
            if (star.camera == cache.camera && windowSize == cache.windowSize) return
            cache.camera = star.camera
            cache.windowSize = windowSize
            camera.x = windowSize.width / 2
            camera.y = Math.hypot(star.camera.x, star.camera.y) + windowSize.height / 2
            camera.rotation = -Math.atan2(star.camera.x, -star.camera.y)
            preprocessor.postMessage({
                camera: star.camera,
                windowSize
            })
        })
        return () => {
            storeUnsub()
            seqSub.unsubscribe()
            app.destroy()
            preprocessor.terminate()
        }
    }, [canvas])
    console.log("rendered at ", Date.now())
    return <canvas ref={setCanvas}></canvas>
}
