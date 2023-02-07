import React from "react";

export interface UIElement {
    type: string
    children: UIElement[]
    style: React.CSSProperties
    callback?: any
    stateName?: string
}