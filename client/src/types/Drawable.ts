import {MakeADT, makeMatchers} from 'ts-adt/MakeADT'

interface Base { x: number, y: number, angle: number }
interface Resource { bundle: string, asset: string }

export type Drawable = MakeADT<'@type', {
    Drawable$Sprite: Base & Resource
    Drawable$Text: Base & { text: string, style: any }
    Drawable$Container: Base & { children: Drawable[] }
    Drawable$AnimatedSprite: Base & Resource & { animation: string, playing: boolean, initialFrame: number }
}>

export const [match, matchP, matchI, matchPI] = makeMatchers('@type')
