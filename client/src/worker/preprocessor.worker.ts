import {State, StateEvent} from "../types/Worker"
import {applyObjectDiff, FPS, subtractSet} from "../utils/common"
import {Subject, throttleTime} from "rxjs"
import {Drawable} from "../types/Drawable"

const state: State = {
    camera: { x: 0, y: 0, rotation: 0 },
    windowSize: { height: 0, width: 0 },
}

const drawables = new Map<string, Drawable>(), changed = new Set<string>()
let prev = new Set<string>()

const update$ = new Subject<any>()

onmessage = async (e: MessageEvent<StateEvent>) => {
    switch (e.data.type) {
        case 'patch': {
            // @ts-ignore
            delete e.data.type
            applyObjectDiff(state, e.data)
            if (!e.data.camera) update$.next(null)
            break
        }
        case 'draw': {
            if (e.data.drawables == undefined) break
            for (const k in e.data.drawables) {
                const d = drawables.get(k), vd = e.data.drawables[k]
                if (!vd) {
                    drawables.delete(k)
                    // if (k.length == 36) console.log("delete", k)
                }
                else if (!d) drawables.set(k, vd)
                else applyObjectDiff(d, vd)
                changed.add(k)
            }
            update$.next(null)
            break
        }
        case 'clear': {
            drawables.clear()
            prev.clear()
            update$.next(null)
            return
        }
        default: return
    }
}

update$.pipe(throttleTime(700 / FPS)).subscribe( () => {
    const res = new Set<string>()
    const wh = state.windowSize.height * 0.55
    const ww = state.windowSize.width * 0.55
    const cx = state.camera.x, cy = state.camera.y
    const cr = Math.hypot(cx, cy)
    const nx = cx / cr, ny = cy / cr
    const r = Math.hypot(state.windowSize.height, state.windowSize.width) * 0.6
    drawables.forEach((d, k) => {
        // TODO: 加上 screen rotation
        // const x_ = d.x - cx, y_ = d.y - cy
        // const dx = - x_ * ny + y_ * nx, dy = - x_ * nx - y_ * ny
        // if (Math.abs(dx) < ww && Math.abs(dy) < wh) res.add(k)
        if (Math.hypot(d.x - state.camera.x, d.y - state.camera.y) < r) res.add(k)
    })
    const add = new Set<string>(subtractSet(res, prev)),
        remove = new Set<string>(subtractSet(prev, res))
    for (const c of changed) {
        const d = drawables.get(c)
        if (d && res.has(c)) add.add(c)
        if (!d && prev.has(c)) remove.add(c)
    }
    prev = res
    if (!add.size && !remove.size) return
    const data = new Map<string, Drawable | null>()
    for (const s of add) data.set(s, drawables.get(s) ?? null)
    for (const s of remove) data.set(s, null)
    postMessage({
        modify: data,
        camera: state.camera
    })
    changed.clear()
})
