export interface Drawable {
    "@type": "Drawable$Sprite" | "Drawable$Text"
    x: number
    y: number
    angle: number
}

export interface Sprite extends Drawable {
    "@type": "Drawable$Sprite"
    bundle: string,
    asset: string
}

export interface Text extends Drawable {
    "@type": "Drawable$Text"
    text: string
    style: any
}
