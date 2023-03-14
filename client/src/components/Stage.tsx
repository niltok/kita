import React, {useEffect, useState} from "react"
import {keyEvents$, mouseEvents$, sendSocket$, seqDrawables$, workerCommon$} from "../dbus"
import {Drawable} from "../types/Drawable"
import {FPS, getKeyCode} from "../utils/common"
import {store} from "../store"
import {useKeyboard, useMouse, useSubscribe, useWindowSize} from "../utils/react";
import {renderer} from "../worker/workers";
import {useAppSelector} from "../storeHook";
import {throttleTime} from "rxjs";

export type SeqDrawable = { data: { [key: string]: Drawable } };

type KeyType = string | { action: string, value: number } | { type: string, [key: string]: any };
const keyMapper: { [key: string]: KeyType } = {
    "KeyW": {action: "up", value: 2},
    "KeyW!": "up",
    "KeyS": {action: "down", value: 2},
    "KeyS!": "down",
    "KeyA": {action: "left", value: 2},
    "KeyA!": "left",
    "KeyD": {action: "right", value: 2},
    "KeyD!": "right",
    "KeyM": {type: "starMap.toggle"},
    "Space": "jump"
}

function handleKeyEvent(e: KeyboardEvent, mapper: { [key: string]: KeyType }) {
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
    useKeyboard()
    useMouse()
    useWindowSize()
    useSubscribe(keyEvents$, e => handleKeyEvent(e, keyMapper))
    const [canvas, setCanvas] = useState<HTMLCanvasElement | null>(null)
    const [info, setInfo] = useState<any>({})
    const camera = useAppSelector(state => state.gameState.star.camera)
    useSubscribe(mouseEvents$.pipe(throttleTime(1000 / FPS)), e => {
        // sendSocket$.next({})
    })
    useEffect(() => {
        if (canvas == null) return
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
            workerCommon$.next({
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
        renderer.postMessage({
            type: 'canvas',
            canvas: offscreen,
            windowSize: {
                height: windowSize.height * window.devicePixelRatio,
                width: windowSize.width * window.devicePixelRatio
            }
        }, [offscreen])
        const seqSub = seqDrawables$.subscribe({
            next(val) {
                workerCommon$.next({
                    type: 'draw',
                    drawables: val.data
                })
            }
        })
        return () => {
            workerCommon$.next({ type: 'clear' })
            storeUnsub()
            seqSub.unsubscribe()
        }
    }, [canvas])
    return (<>
        <div className={"absolute debug-info"}>{JSON.stringify(info)}</div>
        <canvas ref={setCanvas}></canvas>
    </>)
}
