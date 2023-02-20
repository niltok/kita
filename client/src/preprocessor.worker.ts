import {State, StateEvent} from "./types/Preprocessor"
import {applyObjectDiff, FPS, subtractSet} from "./utils/common"
import {debounceTime, Subject} from "rxjs";
import {Drawable, matchI} from "./types/Drawable";
import {manifest} from "./manifest";
import * as pixi from '@pixi/webworker'

const state: State = {
    camera: { x: 0, y: 0 },
    drawables: new Map<string, Drawable>(),
    windowSize: { height: 0, width: 0 },
    cache: new Map<string, pixi.DisplayObject>(),
}

const update$ = new Subject<any>()

onmessage = async (e: MessageEvent<StateEvent>) => {
    switch (e.data.type) {
        case 'canvas': {
            await pixi.Assets.init({manifest})
            state.windowSize = e.data.windowSize!
            state.app = new pixi.Application({
                height: state.windowSize.height,
                width: state.windowSize.width,
                view: e.data.canvas,
                antialias: false,
            })
            const app = state.app
            app.ticker.maxFPS = FPS
            const camera = new pixi.Container()
            state.cameraView = camera
            app.stage.addChild(camera)
            break
        }
        case 'patch': {
            // @ts-ignore
            delete e.data.type
            applyObjectDiff(state, e.data)
            break
        }
        case 'draw': {
            if (e.data.drawables != undefined) {
                for (const k in e.data.drawables) {
                    const d = state.drawables.get(k), vd = e.data.drawables[k]
                    if (vd === null) state.drawables.delete(k)
                    else if (d === undefined) state.drawables.set(k, vd)
                    else applyObjectDiff(d, vd)
                    const cache = state.cache.get(k)
                    if (cache) await renderDrawable(vd, cache)
                }
            }
            break
        }
        default: return
    }
    update$.next({})
}

update$.pipe(debounceTime(10)).subscribe(async () => {
    // console.log(state)
    if (!state.app || !state.cameraView) return
    state.app.view.height = state.windowSize.height
    state.app.view.width = state.windowSize.width
    const res = new Set<string>()
    const r = Math.hypot(state.windowSize.height, state.windowSize.width) * 0.6
    state.drawables.forEach((d, k) => {
        if (Math.hypot(d.x - state.camera.x, d.y - state.camera.y) < r) res.add(k)
    })
    const prev = new Set(state.cache.keys())
    const data = {
        add: subtractSet(res, prev),
        delete: subtractSet(prev, res)
    }
    for (const key of data.add) {
        const cache = state.cache.get(key)
        const display = await renderDrawable(state.drawables.get(key)!, cache)
        if (!cache) {
            await state.cameraView.addChild(display)
            state.cache.set(key, display)
        }
    }
    for (const key of data.delete) {
        state.cameraView.removeChild(state.cache.get(key)!)
        state.cache.delete(key)
    }
    state.cameraView.sortableChildren = true
    state.cameraView.sortChildren()
    state.cameraView.sortableChildren = false
    state.cameraView.x = state.windowSize.width / 2
    state.cameraView.y = Math.hypot(state.camera.x, state.camera.y) + state.windowSize.height / 2
    state.cameraView.rotation = -Math.atan2(state.camera.x, -state.camera.y)
})

function renderDrawable(drawable: Drawable, cache?: pixi.DisplayObject) {
    const setCommonProp = (display: pixi.DisplayObject) => {
        display.x = drawable.x
        display.y = drawable.y
        display.zIndex = drawable.zIndex
        display.rotation = drawable.angle
        display.pivot.set(0.5, 0.5)
        if (display instanceof pixi.Sprite) display.anchor.set(.5, .5)
    }
    return matchI(drawable) ({
        async Sprite(sprite) {
            const display = cache as pixi.Sprite || new pixi.Sprite()
            display.texture = (await pixi.Assets.loadBundle(sprite.bundle))[sprite.asset]
            setCommonProp(display)
            return display
        },
        async Text(text) {
            const display = cache as pixi.Text || new pixi.Text()
            display.text = text.text
            display.style = {
                fontFamily: ["Sourcehanserifcn Vf.ttf"],
                ...text.style
            }
            setCommonProp(display)
            return display
        },
        async Container(container) {
            const display = cache as pixi.Container || new pixi.Container()
            display.removeChildren()
            for (const d of container.children) {
                await display.addChild(await renderDrawable(d));
            }
            display.sortableChildren = true
            display.sortChildren()
            display.sortableChildren = false
            setCommonProp(display)
            return display
        },
        async AnimatedSprite(animated) {
            const display = cache as pixi.AnimatedSprite || new pixi.AnimatedSprite(
                (await pixi.Assets.loadBundle(animated.bundle))[animated.asset].animations[animated.animation])
            display.textures = (await pixi.Assets.loadBundle(animated.bundle))[animated.asset].animations[animated.animation]
            if (animated.playing) display.gotoAndPlay(animated.initialFrame)
            else display.gotoAndStop(animated.initialFrame)
            setCommonProp(display)
            return display
        }
    })
}
