import {UIElement} from "../types/UIElement";
import {sendSocket$} from "../dbus";
import {store} from "../store";
import {diffGame} from "../stores/gameState";
import React from "react";

export function renderUI(elem: UIElement | undefined, uiState: {[key: string]: any}) {
    if (!elem) return <></>
    const commonProp = elem && {
        style: elem.style,
        className: elem.classes.join(' '),
        children: elem.children.length == 0 ? undefined : elem.children.filter(e => e).map(e => renderUI(e, uiState)),
        title: elem.title ?? undefined
    }
    const clickCallback = elem && elem.callback && JSON.stringify(elem.callback) != "{}" ? {
        onClick: () => {
            let states: {[key: string]: any} = {}
            if (elem.states) elem.states.forEach(s => states[s] = store.getState().gameState.uiState[s])
            sendSocket$.next({
                ...elem.callback,
                states
            })
        }
    } : {}
    const changeHandler = elem && elem.stateName ? (e: React.ChangeEvent<HTMLInputElement>) => {
        console.log(elem.stateName, e.target.value)
        if (elem.stateName) store.dispatch(diffGame({uiState: {[elem.stateName]: e.target.value}}))
    } : () => {}
    const inputProp = elem && elem.stateName ? {
        // value: uiState[elem.stateName],
        onChange: changeHandler
    } : {}
    switch (elem.type) {
        case "div":
            return (<div {...commonProp} {...clickCallback} />)
        case "span":
            return (<span {...commonProp} {...clickCallback} />)
        case "button":
            return (<button {...commonProp} {...clickCallback} />)
        case "input.text":
            return (<input type={"text"} {...commonProp} onChange={e => changeHandler(e)} />)
        case "input.number":
            return (<input type={"number"} {...commonProp} onChange={e => changeHandler(e)} />)
        case "text":
            return <>{elem.text}</>
        case "br":
            return <br/>
        default:
            return <>{elem.type}</>
    }
}