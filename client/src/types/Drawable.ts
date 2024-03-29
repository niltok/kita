import {MakeADT, makeMatchers} from 'ts-adt/MakeADT'

interface Base { x: number, y: number, rotation: number, zIndex: number }
interface Resource { bundle: string, asset: string }

export type Drawable = MakeADT<'@type', {
    Sprite: Base & Resource
    Text: Base & { text: string, style: any }
    Container: Base & { children: Drawable[] }
    AnimatedSprite: Base & Resource & { animation: string, playing: boolean, initialFrame: number }
    Line: Base & { length: number, width: number, color: number }
}>

export const [match, matchP, matchI, matchPI] = makeMatchers('@type')
