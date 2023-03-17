import {State, StateEvent} from "../types/Worker"
import {applyObjectDiff, FPS, subtractSet} from "../utils/common"
import {Subject, throttleTime} from "rxjs"
import {Drawable} from "../types/Drawable"

const state: State = {
    camera: { x: 0, y: 0 },
    windowSize: { height: 0, width: 0 },
}

const drawables = new Map<string, Drawable>()
let prev = new Set<string>()

const update$ = new Subject<any>()

onmessage = async (e: MessageEvent<StateEvent>) => {
    switch (e.data.type) {
        case 'patch': {
            // @ts-ignore
            delete e.data.type
            applyObjectDiff(state, e.data)
            update$.next(null)
            break
        }
        case 'draw': {
            if (e.data.drawables != undefined) {
                for (const k in e.data.drawables) {
                    const d = drawables.get(k), vd = e.data.drawables[k]
                    if (vd === null) drawables.delete(k)
                    else if (d === undefined) drawables.set(k, vd)
                    else applyObjectDiff(d, vd)
                }
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

update$.pipe(throttleTime(1000 / FPS)).subscribe( () => {
    const res = new Set<string>()
    const wh = state.windowSize.height * 0.51
    const ww = state.windowSize.width * 0.51
    const cx = state.camera.x, cy = state.camera.y
    const cr = Math.hypot(cx, cy)
    const nx = cx / cr, ny = cy / cr
    drawables.forEach((d, k) => {
        const x_ = d.x - cx, y_ = d.y - cy
        const dx = - x_ * ny + y_ * nx, dy = - x_ * nx - y_ * ny
        if (Math.abs(dx) < ww && Math.abs(dy) < wh) res.add(k)
    })
    const data = ({
        add: subtractSet(res, prev),
        delete: subtractSet(prev, res)
    })
    prev = res
    if (data.add.length || data.delete.length) postMessage(data)
})
