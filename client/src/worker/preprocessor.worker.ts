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
            break
        }
        case 'clear': {
            drawables.clear()
            prev.clear()
            return
        }
        default: return
    }
}

setInterval( () => {
    const res = new Set<string>()
    const r = Math.hypot(state.windowSize.height, state.windowSize.width) * 0.6
    drawables.forEach((d, k) => {
        if (Math.hypot(d.x - state.camera.x, d.y - state.camera.y) < r) res.add(k)
    })
    const data = ({
        add: subtractSet(res, prev),
        delete: subtractSet(prev, res)
    })
    prev = res
    if (data.add.length || data.delete.length) postMessage(data)
}, 1)
