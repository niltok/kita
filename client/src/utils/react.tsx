import {useEffect, useState} from "react";
import {keyEvents$, mouseEvents$} from "../dbus";
import {Observable} from "rxjs";
import {useDiffGame} from "../stores/gameState";
import {isDraft, original} from "@reduxjs/toolkit";

export function useAsyncEffect(effect: () => Promise<void | (() => void)>, dependencies?: any[]) {
    return useEffect(() => {
        const cleanupPromise = effect()
        return () => {
            cleanupPromise.then(cleanup => cleanup && cleanup())
        }
    }, dependencies)
}

export function useRefresh(initCount: number = -1): [boolean, number, () => void, () => void] {
    const [flag, setFlag] = useState(false)
    const [count, setCount] = useState(initCount)
    return [flag, count, () => {
        setCount(count => count > 0 ? count - 1 : count)
        setFlag(flag => !flag)
    }, () => setCount(initCount)]
}

export function useSubscribe<T>(obs: Observable<T>, callback: (val: T) => void) {
    useEffect(() => {
        const sub = obs.subscribe({
            next(val) {
                callback(val)
            }
        });
        return () => sub.unsubscribe()
    }, [obs, callback])
}

export function useObservable<T>(obs: Observable<T>, init: T): T {
    const [value, setValue] = useState(init)
    useSubscribe(obs, setValue);
    return value
}

export function useKeyboard() {
    useEffect(() => {
        function handleKeyEvent(e: KeyboardEvent) {
            e.preventDefault()
            if (e.repeat) return
            keyEvents$.next(e)
        }
        document.body.addEventListener('keydown', handleKeyEvent)
        document.body.addEventListener('keyup', handleKeyEvent)
        return () => {
            document.body.removeEventListener('keydown', handleKeyEvent)
            document.body.removeEventListener('keyup', handleKeyEvent)
        }
    }, [])
}

export function useMouse() {
    useEffect(() => {
        function handleKeyEvent(e: PointerEvent) {
            if (!e.isPrimary) return
            mouseEvents$.next(e)
        }
        document.body.addEventListener('pointermove', handleKeyEvent, {
            passive: true
        })
        return () => {
            document.body.removeEventListener('pointermove', handleKeyEvent)
        }
    }, [])
}

export function useWindowSize() {
    const diffGame = useDiffGame()
    useEffect(() => {
        const listener = () => {
            diffGame({
                windowSize: {
                    height: document.body.clientHeight,
                    width: document.body.clientWidth
                }
            })
        }
        listener()
        window.addEventListener('resize', listener)
        return () => window.removeEventListener('resize', listener)
    }, [])
}

export function applyReduxDiff(obj: any, diff: { [key: string]: any }) {
    for (const ptr in diff) {
        const val = diff[ptr]
        if (val === null) delete obj[ptr]
        else {
            let elem = obj[ptr];
            if (typeof val == 'object' && elem !== undefined && elem !== null
                && (isDraft(elem) && typeof original(elem) == 'object' || typeof elem == 'object'))
                applyReduxDiff(elem, val)
            else obj[ptr] = val
        }
    }
}