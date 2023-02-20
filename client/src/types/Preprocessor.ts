import {Drawable} from "./Drawable"
import {Pretty} from "../utils/common";
import {Application, Container, DisplayObject} from "pixi.js";

interface Base {
    camera: { x: number, y: number }
    windowSize: { height: number, width: number }
    canvas?: OffscreenCanvas
}

export interface State extends Base {
    drawables: Map<string, Drawable>
    app?: Application
    cameraView?: Container
    cache: Map<string, DisplayObject>
}

export type StateEvent = Pretty<{ type: string } & Partial<Base & {
    drawables: { [key: string]: Drawable }
}>>
export type ModifyEvent = {
    add: string[]
    delete: string[]
}
