import React, {useEffect, useState} from "react"
import {keyEvents$, mouseEvents$, sendSocket$, seqDrawables$, workerCommon$} from "../dbus"
import {Drawable} from "../types/Drawable"
import {FPS, getKeyCode, getMouseCode} from "../utils/common"
import {store} from "../store"
import {useKeyboard, useMouse, useSubscribe, useWindowSize} from "../utils/react"
import {renderer} from "../worker/workers"
import {useAppSelector} from "../storeHook"
import {pipe, throttleTime} from "rxjs"
import './Stage.css'
import {keyMapper, KeyType} from "../keyMapper";

export type SeqDrawable = { data: { [key: string]: Drawable } };

function handleInputEvent(mapper: { [key: string]: KeyType }, code: string) {
    let action = mapper[code]
    while (typeof action == 'undefined' && code.length > 0 && "$#@^".indexOf(code.at(0)!) != -1) {
        code = code.substring(1)
        action = mapper[code]
    }
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
    const [canvas, setCanvas] = useState<HTMLCanvasElement | null>(null)
    const [info, setInfo] = useState<any>({})
    const windowSize = useAppSelector(state => state.gameState.windowSize)
    useKeyboard(canvas)
    useMouse(canvas)
    useWindowSize()
    useSubscribe(keyEvents$, e => handleInputEvent(keyMapper, getKeyCode(e)))
    useSubscribe(mouseEvents$, e => handleInputEvent(keyMapper, getMouseCode(e)))
    useSubscribe(mouseEvents$.pipe(throttleTime(1000 / FPS)), e => {
        if (!windowSize) return
        const dx = (e.clientX - windowSize.width / 2) * window.devicePixelRatio
        const dy = (e.clientY - windowSize.height / 2) * window.devicePixelRatio
        if (dx && dy) sendSocket$.next({
            type: "star.operate.mouse",
            x: dx, y: dy
        })
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
    return (<div>
        {/*<div className={"absolute debug-info"}>{JSON.stringify(info)}</div>*/}
        <canvas ref={setCanvas}></canvas>
    </div>)
}
