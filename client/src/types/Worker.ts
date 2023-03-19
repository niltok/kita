import {Drawable} from "./Drawable"
import {Pretty} from "../utils/common";

interface Base {
    camera: { x: number, y: number, rotation: number }
    windowSize: { height: number, width: number }
    canvas?: OffscreenCanvas
}

export interface State extends Base {
}

export type StateEvent = Pretty<{ type: string } & Partial<Base & ModifyEvent & {
    drawables: { [key: string]: Drawable },
}>>
export type ModifyEvent = {
    modify: Map<string, Drawable | null>
    camera: { x: number, y: number, rotation: number }
}
