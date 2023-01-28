import {useAppDispatch, useAppSelector} from "../storeHook"
import {diffGame, selectGameState} from "../stores/gameState"
import SockJS from "sockjs-client"
import {useAsyncEffect, useRefresh} from "../utils"
import {AppDispatch} from "../store"
import {sendSocket$, setPage$} from "../dbus"
import {Subscription} from "rxjs"

export default function Socket(prop: {children?: JSX.Element}) {
    const { token, url } = useAppSelector(selectGameState)
    const dispatch = useAppDispatch()
    const [flag, refresh] = useRefresh()
    useAsyncEffect(async () => {
        if (!url || !token) return
        const socket = new SockJS(url + '/socket')
        let subscribe: Subscription | null = null
        socket.onmessage = e => {
            onMsg(socket, JSON.parse(e.data), dispatch)
        }
        socket.onclose = e => {
            if (subscribe != null) subscribe.unsubscribe()
            console.warn({
                warn: "socket closed",
                reason: e.reason,
                code: e.code
            })
            dispatch(diffGame({'/socket': null}))
            if (e.code < 3000) setTimeout(refresh, 3000)
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
    }, [token, flag])
    return prop.children || (<></>)
}

function onMsg(socket: WebSocket, json: any, dispatch: AppDispatch) {
    switch (json['type']) {
        case 'auth.pass': {
            dispatch(diffGame({'/socket': socket}))
            break
        }
        case 'state.dispatch': {
            dispatch({
                type: json.action,
                payload: json.payload
            })
            break
        }
    }
}
