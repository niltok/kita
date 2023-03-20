import {useAppDispatch, useAppSelector} from "../storeHook"
import {diffGame} from "../stores/gameState"
import SockJS from "sockjs-client"
import {AppDispatch} from "../store"
import {sendSocket$, seqDrawables$, setPage$} from "../dbus"
import {Subscription} from "rxjs"
import {useAsyncEffect, useRefresh} from "../utils/react";
import {ReactNode, useEffect} from "react";

export default function Socket(prop: {children?: ReactNode}) {
    const server = useAppSelector(state => state.gameState.server)
    const dispatch = useAppDispatch()
    const [flag, refCount, refresh, reset] = useRefresh(5)
    useEffect(() => reset(), [])
    useAsyncEffect(async () => {
        if (!server) return
        const { token, url } = server
        const socket = new SockJS(url + '/socket')
        let subscribe: Subscription | null = null
        socket.onmessage = e => {
            // console.log(JSON.parse(e.data))
            onMsg(socket, JSON.parse(e.data), dispatch)
        }
        socket.onclose = e => {
            if (subscribe != null) subscribe.unsubscribe()
            console.warn({
                warn: "socket closed",
                reason: e.reason,
                code: e.code
            })
            dispatch(diffGame({ connection: { state: 'failed' } }))
            if (e.code < 3000 && refCount) setTimeout(refresh, 3000)
            else setPage$.next('login')
        }
        socket.onerror = e => {
            console.error({
                error: "socket",
                event: e
            })
            refresh()
        }
        socket.onopen = e => {
            socket.send(JSON.stringify({
                type: "auth.request",
                token
            }))
            subscribe = sendSocket$.subscribe({
                next(msg) {
                    socket.send(JSON.stringify(msg))
                }
            })
        }
        return () => {
            socket.close()
        }
    }, [server?.token, flag])
    return <>{prop.children}</>
}

function onMsg(socket: WebSocket, json: any, dispatch: AppDispatch) {
    if (Array.isArray(json)) {
        json.forEach(e => onMsg(socket, e, dispatch))
        return
    }
    switch (json['type']) {
        case 'auth.pass': {
            dispatch(diffGame({ connection: { state: 'connected' } }))
            break
        }
        case 'state.dispatch': {
            dispatch({
                type: json.action,
                payload: json.payload
            })
            break
        }
        case "seq.operate": {
            seqDrawables$.next(json)
            break
        }
        case "socket.echo": {
            sendSocket$.next(json.payload)
            break
        }
    }
}
