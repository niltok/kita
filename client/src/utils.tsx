import {useEffect, useState} from "react"
import {Observable} from "rxjs"
import {Drawable, matchI} from "./types/Drawable"
import {_ReactPixi, AnimatedSprite, Container, Sprite, Text} from "@inlet/react-pixi"
import {TextStyle} from 'pixi.js'

export const delay = (time: number) => {
    return new Promise(resolve => setTimeout(resolve, time))
}

export function useAsyncEffect(effect: () => Promise<void | (() => void)>, dependencies?: any[]) {
    return useEffect(() => {
        const cleanupPromise = effect()
        return () => { cleanupPromise.then(cleanup => cleanup && cleanup()) }
    }, dependencies)
}

export function useRefresh(): [boolean, () => void] {
    const [flag, setFlag] = useState(false)
    return [flag, () => setFlag(flag => !flag)]
}

export function useSubscribe<T>(obs: Observable<T>, callback: (val: T) => void) {
    useEffect(() => {
        const sub = obs.subscribe({
            next(val) { callback(val) }
        });
        return () => sub.unsubscribe()
    })
}

export function useObservable<T>(obs: Observable<T>, init: T): T {
    const [value, setValue] = useState(init)
    useSubscribe(obs, setValue);
    return value
}

export function renderDrawables(drawables: Drawable[], assets: any) {
    return drawables.map(drawable => {
        const containerProp = {
            anchor: 0.5,
            position: [drawable.x, drawable.y] as _ReactPixi.PointLike,
            rotation: drawable.angle
        }
        const key = JSON.stringify(drawable)
        return matchI(drawable) ({
            Drawable$Sprite: sprite => {
                return <Sprite {...containerProp}
                    key={key}
                    texture={assets[sprite.bundle][sprite.asset]}
                />
            },
            Drawable$Text: text => {
                return <Text {...containerProp}
                    key={key}
                    text={text.text}
                    style={new TextStyle({
                        fontFamily: "Source Han Serif CN VF",
                        ...text.style
                    })}
                />
            },
            Drawable$Container: container => {
                return <Container {...containerProp} key={key}>
                    {renderDrawables(container.children, assets)}
                </Container>
            },
            Drawable$AnimatedSprite: animated => {
                return <AnimatedSprite {...containerProp}
                    key={key}
                    textures={assets[animated.bundle][animated.asset].animations[animated.animation]}
                    isPlaying={animated.playing}
                    initialFrame={animated.initialFrame}
                />
            }
        })
    })
}

/// 前缀（注意顺序）：$(Ctrl), #(Meta), @(Alt), ^(Shift)
//
// 后缀：!(keyup)
//
// 举例：$^Digit1! 表示Ctrl+Shift+1这个组合键抬起
export function getKeyCode(e: KeyboardEvent) {
    let keyCode = ''
    if (e.ctrlKey) keyCode += '$'
    if (e.metaKey) keyCode += '#'
    if (e.altKey) keyCode += '@'
    if (e.shiftKey) keyCode += '^'
    keyCode += e.code
    if (e.type == 'keyup') keyCode += '!'
    return keyCode
}