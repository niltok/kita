import {State, StateEvent} from "./types/Preprocessor"
import {applyObjectDiff, FPS, subtractSet} from "./utils/common"
import {debounceTime, Subject} from "rxjs";

const state: State = {
    camera: { x: 0, y: 0 },
    drawables: new Map<string, {x: number, y: number}>(),
    windowSize: { height: 0, width: 0 },
    prev: new Set<string>()
}

const update$ = new Subject<any>()

onmessage = (e: MessageEvent<StateEvent>) => {
    if (e.data.drawables != undefined) {
        for (const k in e.data.drawables) {
            const d = state.drawables.get(k), vd = e.data.drawables[k]
            if (vd === null) state.drawables.delete(k)
            else if (d === undefined) state.drawables.set(k, vd)
            else applyObjectDiff(d, vd)
        }
        delete e.data.drawables
    }
    applyObjectDiff(state, e.data)
    update$.next({})
}

update$.pipe(debounceTime(1000 / FPS)).subscribe(() => {
    const res = new Set<string>()
    const r = Math.max(1000, Math.hypot(state.windowSize.height, state.windowSize.width))
    state.drawables.forEach((d, k) => {
        if (Math.hypot(d.x - state.camera.x, d.y - state.camera.y) < r) res.add(k)
    })
    postMessage({
        add: subtractSet(res, state.prev),
        delete: subtractSet(state.prev, res)
    })
    state.prev = res
})
