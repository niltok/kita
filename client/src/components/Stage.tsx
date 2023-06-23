import React, {memo, useCallback, useEffect, useMemo, useState} from "react"
import {keyEvents$, mouseEvents$, sendSocket$, seqDrawables$, wheelEvents$, workerCommon$} from "../dbus"
import {Drawable} from "../types/Drawable"
import {FPS, getKeyCode, getMouseCode} from "../utils/common"
import {store} from "../store"
import {useKeyboard, useMouse, useSubscribe, useWindowSize} from "../utils/react"
import {preprocessor, renderer} from "../worker/workers"
import {useAppSelector} from "../storeHook"
import {throttleTime} from "rxjs"
import './Stage.css'
import {keyMapper, KeyType} from "../keyMapper";
import {useDiffGame} from "../stores/gameState";

export type SeqDrawable = { data: { [key: string]: Drawable } };

function handleInputEvent(mapper: { [key: string]: KeyType }, code: string) {
    let action = mapper[code]
    while (typeof action == 'undefined' && code.length > 0 && "$#@^".indexOf(code.at(0)!) != -1) {
        code = code.substring(1)
        action = mapper[code]
    }
    while (action instanceof Function) {
        action = action()
    }
    if (typeof action == 'undefined') return
    if (typeof action == 'string') {
        sendSocket$.next({
            type: "star.operate.key",
            action
        })
        return;
    }
    if (typeof action == 'object') {
        sendSocket$.next({
            type: "star.operate.key",
            ...action
        })
        return;
    }
}

export const Stage = () => {
    const [canvas, setCanvas] =
        useState<HTMLCanvasElement | null>(null)
    const windowSize =
            useAppSelector(state => state.gameState.windowSize)
    const differ = useDiffGame()
    useKeyboard(document.body)
    useMouse(canvas)
    useWindowSize()
    useSubscribe(keyEvents$, useCallback(e =>
        handleInputEvent(keyMapper, getKeyCode(e)), [keyMapper]))
    useSubscribe(mouseEvents$, useCallback(e =>
        handleInputEvent(keyMapper, getMouseCode(e)), [keyMapper]))
    console.log('reload stage')
    useSubscribe(useMemo(() => wheelEvents$.pipe(throttleTime(200)), [wheelEvents$]),
        useCallback(e =>
        handleInputEvent(keyMapper, getMouseCode(e)), [keyMapper]))
    useSubscribe(useMemo(() => mouseEvents$.pipe(throttleTime(1000 / FPS)), [mouseEvents$]),
        useCallback(e => {
        if (!windowSize) return
        const dx = (e.clientX - windowSize.width / 2) * window.devicePixelRatio
        const dy = (e.clientY - windowSize.height / 2) * window.devicePixelRatio
        if (dx && dy) sendSocket$.next({
            type: "star.operate.mouse",
            x: dx, y: dy
        })
    }, [sendSocket$, windowSize]))
    useEffect(() => {
        differ({ star: { ui: undefined } } as any)
        if (canvas == null) return
        const cache: any = {
            camera: null,
            windowSize: null
        }
        const storeUnsub = store.subscribe(() => {
            const { star, windowSize } =
                store.getState().gameState
            if (star.camera == cache.camera && windowSize == cache.windowSize) return
            cache.camera = star.camera
            cache.windowSize = windowSize
            // canvas.style.height = `${windowSize.height}px`
            // canvas.style.width = `${windowSize.width}px`
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
        canvas.style.height = `100vh`
        canvas.style.width = `100vw`
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
                preprocessor.postMessage({
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
        <canvas ref={setCanvas} className={"no-cursor"}></canvas>
    </>)
}

export const PureStage = memo(Stage)
