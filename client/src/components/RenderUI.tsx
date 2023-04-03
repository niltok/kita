import {UIElement} from "../types/UIElement";
import {sendSocket$} from "../dbus";
import {store} from "../store";
import {diffGame} from "../stores/gameState";
import React from "react";

export function renderUI(elem: UIElement | undefined): JSX.Element {
    if (!elem) return <></>
    const commonProp = {
        style: elem.style,
        className: elem.classes.join(' '),
        children: elem.children.map(e => renderUI(e)),
        title: elem.title ?? undefined
    }
    const clickCallback = elem.callback && JSON.stringify(elem.callback) != "{}" ? {
        onClick: () => sendSocket$.next(elem.callback)
    } : {}
    switch (elem.type) {
        case "div":
            return (<div {...commonProp} {...clickCallback} />)
        case "span":
            return (<span {...commonProp} {...clickCallback} />)
        case "button":
            return (<button {...commonProp} {...clickCallback} />)
        case "input.text":
            return (<input type={"text"} {...commonProp} onChange={e =>
                elem.stateName && store.dispatch(diffGame({uiState: {[elem.stateName]: e.target.value}}))}/>)
        case "text":
            return <>{elem.text}</>
        case "br":
            return <br/>
        default:
            return <>{elem.type}</>
    }
}