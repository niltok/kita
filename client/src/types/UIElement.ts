import React from "react";

export interface UIElement {
    type: string
    children: UIElement[]
    style: React.CSSProperties
    classes: string[]
    callback?: any
    states?: string[]
    stateName?: string
    value?: any
    text?: string
    title?: string
}