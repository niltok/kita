export interface UIElement {
    type: string
    children: UIElement[]
    style: string
    classes: string[]
    callback?: string
    states?: string[]
    stateName?: string
    value?: any
    text?: string
    title?: string
}