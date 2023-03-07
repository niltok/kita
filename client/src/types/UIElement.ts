import React from "react";

export interface UIElement {
    type: string
    children: UIElement[]
    style: React.CSSProperties
    classes: string[]
    callback?: any
    stateName?: string
    text?: string
}