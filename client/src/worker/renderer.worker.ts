import {State, StateEvent} from "../types/Worker"
import {applyObjectDiff, FPS} from "../utils/common"
import {Drawable, matchI} from "../types/Drawable";
import {manifest} from "../manifest";
import * as pixi from '@pixi/webworker'

const state: State = {
    camera: { x: 0, y: 0 },
    windowSize: { height: 0, width: 0 },
}

const cache = new Map<string, pixi.DisplayObject>()
let app: pixi.Application | null = null
let camera: pixi.Container | null = null
const assets: any = {}

onmessage = async (e: MessageEvent<StateEvent>) => {
    switch (e.data.type) {
        case 'init': {
            await pixi.Assets.init({manifest})
            await Promise.all(manifest.bundles.map(async bundle => {
                assets[bundle.name] = await pixi.Assets.loadBundle(bundle.name)
            }))
            postMessage({
                type: 'init.end'
            })
            break
        }
        case 'canvas': {
            state.windowSize = e.data.windowSize!
            app = new pixi.Application({
                height: state.windowSize.height,
                width: state.windowSize.width,
                view: e.data.canvas,
                antialias: false,
                backgroundColor: "000030" // "00A0E8"
            })
            app.ticker.maxFPS = FPS
            camera = new pixi.Container()
            app.stage.addChild(camera)
            break
        }
        case 'patch': {
            // @ts-ignore
            delete e.data.type
            applyObjectDiff(state, e.data)
            if (!app || !camera) return
            app.view.height = state.windowSize.height
            app.view.width = state.windowSize.width
            break
        }
        case 'preprocessed': {
            if (!app || !camera) return
            const map = e.data.modify!
            map.forEach((drawable, key) => {
                if (drawable) {
                    const cached = cache.get(key)
                    try {
                        const display = renderDrawable(drawable!, cached)
                        if (!cached) {
                            camera!.addChild(display)
                            cache.set(key, display)
                        }
                    } catch (e) {
                        console.log(e, { key, drawable })
                    }
                } else {
                    camera!.removeChild(cache.get(key)!)
                    cache.delete(key)
                }
            })
            camera.sortableChildren = true
            camera.sortChildren()
            camera.sortableChildren = false
            camera.x = state.windowSize.width / 2
            camera.y = Math.hypot(state.camera.x, state.camera.y) + state.windowSize.height / 2
            camera.rotation = -Math.atan2(state.camera.x, -state.camera.y)
            break
        }
        case 'clear': {
            cache.clear()
            camera = null
            app?.destroy()
            app = null
            break
        }
        default: return
    }
}

function renderDrawable(drawable: Drawable, cache?: pixi.DisplayObject) {
    const setCommonProp = (display: pixi.DisplayObject) => {
        display.x = drawable.x
        display.y = drawable.y
        display.zIndex = drawable.zIndex
        display.rotation = drawable.rotation
        display.pivot.set(0.5, 0.5)
        if (display instanceof pixi.Sprite) display.anchor.set(.5, .5)
    }
    return matchI(drawable) ({
        Sprite(sprite) {
            const display = cache as pixi.Sprite || new pixi.Sprite()
            display.texture = assets[sprite.bundle][sprite.asset]
            setCommonProp(display)
            return display
        },
        Text(text) {
            const display = cache as pixi.Text || new pixi.Text()
            display.text = text.text
            display.style = {
                fontFamily: ["Sourcehanserifcn Vf.ttf"],
                ...text.style
            }
            setCommonProp(display)
            return display
        },
        Container(container) {
            const display = cache as pixi.Container || new pixi.Container()
            display.removeChildren()
            for (const d of container.children) {
                display.addChild(renderDrawable(d));
            }
            display.sortableChildren = true
            display.sortChildren()
            display.sortableChildren = false
            setCommonProp(display)
            return display
        },
        AnimatedSprite(animated) {
            const display = cache as pixi.AnimatedSprite || new pixi.AnimatedSprite(
                assets[animated.bundle][animated.asset].animations[animated.animation])
            display.textures = assets[animated.bundle][animated.asset].animations[animated.animation]
            if (animated.playing) display.gotoAndPlay(animated.initialFrame)
            else display.gotoAndStop(animated.initialFrame)
            setCommonProp(display)
            return display
        }
    })
}
