import {Drawable} from "./Drawable"
import {Pretty} from "../utils/common";

interface Base {
    camera: { x: number, y: number }
    windowSize: { height: number, width: number }
}

export interface State extends Base {
    drawables: Map<string, {x: number, y: number}>
    prev: Set<string>
}

export type StateEvent = Pretty<Partial<Base & { drawables: { [key: string]: Drawable } }>>
export type ModifyEvent = {
    add: string[]
    delete: string[]
}
