import {MakeADT, makeMatchers} from 'ts-adt/MakeADT'

interface Base { key: string, x: number, y: number, angle: number, zIndex: number }
interface Resource { bundle: string, asset: string }

export type Drawable = MakeADT<'@type', {
    Sprite: Base & Resource
    Text: Base & { text: string, style: any }
    Container: Base & { children: Drawable[] }
    AnimatedSprite: Base & Resource & { animation: string, playing: boolean, initialFrame: number }
}>

export const [match, matchP, matchI, matchPI] = makeMatchers('@type')
