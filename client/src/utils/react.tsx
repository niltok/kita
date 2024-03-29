import {useEffect, useState} from "react";
import {keyEvents$, mouseEvents$, wheelEvents$} from "../dbus";
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

export function useKeyboard(target: HTMLElement | null) {
    useEffect(() => {
        if (!target) return
        function handleKeyEvent(e: KeyboardEvent) {
            if (e.target != target) return;
            e.preventDefault()
            if (e.repeat) return
            keyEvents$.next(e)
        }

        const elem = target;
        elem.addEventListener('keydown', handleKeyEvent)
        elem.addEventListener('keyup', handleKeyEvent)
        return () => {
            elem.removeEventListener('keydown', handleKeyEvent)
            elem.removeEventListener('keyup', handleKeyEvent)
        }
    }, [target])
}

export function useMouse(target: HTMLElement | null) {
    useEffect(() => {
        if (!target) return
        function handlePassive(e: PointerEvent | WheelEvent) {
            if (e.type == 'wheel') {
                wheelEvents$.next(e as WheelEvent)
                return
            }
            if (!((e as PointerEvent).isPrimary) || e.target != target) return
            mouseEvents$.next(e as PointerEvent)
        }
        function handlePointer(e: PointerEvent) {
            e.preventDefault()
            handlePassive(e)
        }

        const elem = target;
        elem.addEventListener('pointerdown', handlePointer)
        elem.addEventListener('pointerup', handlePointer)
        elem.addEventListener('pointercancel', handlePointer)
        elem.addEventListener('pointermove', handlePassive, {
            passive: true
        })
        elem.addEventListener('wheel', handlePassive, {
            passive: true
        })
        return () => {
            elem.removeEventListener('pointerdown', handlePointer)
            elem.removeEventListener('pointerup', handlePointer)
            elem.removeEventListener('pointercancel', handlePointer)
            elem.removeEventListener('pointermove', handlePassive)
            elem.removeEventListener('wheel', handlePassive)
        }
    }, [target])
}

export function useWindowSize() {
    const diffGame = useDiffGame()
    useEffect(() => {
        const listener = () => {
            diffGame({
                windowSize: {
                    height: window.innerHeight,
                    width: window.innerWidth
                }
            })
        }
        listener()
        window.addEventListener('resize', listener)
        window.addEventListener('focus', listener)
        return () => {
            window.removeEventListener('resize', listener)
            window.removeEventListener('focus', listener)
        }
    }, [])
}

export function applyReduxDiff(obj: any, diff: { [key: string]: any }) {
    if (Array.isArray(original(obj)) && Array.isArray(diff)) {
        const delta = obj.length - diff.length
        for (let i = 0; i < delta; i++) obj.pop()
    }
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