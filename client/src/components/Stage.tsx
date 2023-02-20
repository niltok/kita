import React, {useEffect, useState} from "react"
import {keyEvents$, sendSocket$, seqDrawables$} from "../dbus"
import {Drawable} from "../types/Drawable"
import {getKeyCode} from "../utils/common"
import {store} from "../store"
import Preprocessor from '../preprocessor.worker?worker'
import {useKeyboard, useSubscribe, useWindowSize} from "../utils/react";

export type SeqDrawable = { data: { [key: string]: Drawable } };
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
    const [info, setInfo] = useState<any>({})
    useEffect(() => {
        if (canvas == null) return
        const preprocessor = new Preprocessor()
        const cache: any = {
            camera: null,
            windowSize: null
        }
        const storeUnsub = store.subscribe(() => {
            const { star, windowSize } = store.getState().gameState
            if (star.camera == cache.camera && windowSize == cache.windowSize) return
            cache.camera = star.camera
            cache.windowSize = windowSize
            canvas.style.height = `${windowSize.height}px`
            canvas.style.width = `${windowSize.width}px`
            preprocessor.postMessage({
                type: 'patch',
                camera: star.camera,
                windowSize: {
                    height: windowSize.height * window.devicePixelRatio,
                    width: windowSize.width * window.devicePixelRatio
                }
            })
        })
        const windowSize = store.getState().gameState.windowSize
        canvas.style.height = `${windowSize.height}px`
        canvas.style.width = `${windowSize.width}px`
        const offscreen = canvas.transferControlToOffscreen()
        preprocessor.postMessage({
            type: 'canvas',
            canvas: offscreen,
            windowSize: {
                height: windowSize.height * window.devicePixelRatio,
                width: windowSize.width * window.devicePixelRatio
            }
        }, [offscreen])
        const seqSub = seqDrawables$.subscribe({
            next(val) {
                preprocessor.postMessage({
                    type: 'draw',
                    drawables: val.data
                })
            }
        })
        return () => {
            storeUnsub()
            seqSub.unsubscribe()
            preprocessor.terminate()
        }
    }, [canvas])
    return (<>
        <div className={"client-info"}>{JSON.stringify(info)}</div>
        <canvas ref={setCanvas}></canvas>
    </>)
}
