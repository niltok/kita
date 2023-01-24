export interface Drawables {
    "@type": string
    x: number
    y: number
    angle: number
}

export interface Sprite extends Drawables {
    "@type": "Drawable$Sprite"
    type: string
}
