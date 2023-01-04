import {useAppDispatch, useAppSelector} from "../storeHook"
import {applyDiff, selectGameState} from "../stores/gameState"
import SockJS from "sockjs-client"
import {useAsyncEffect, useRefresh} from "../utils"
import {useNavigate} from "react-router-dom";
import {AppDispatch} from "../store";

export default function Socket(prop: {children?: JSX.Element}) {
    const { token, url, socket } = useAppSelector(selectGameState)
    const dispatch = useAppDispatch()
    const navi = useNavigate()
    const [flag, refresh] = useRefresh()
    useAsyncEffect(async () => {
        if (!url || !token) return
        const endpoint = await fetch(url + '/endpoint', {
            headers: { token }
        })
        if (!endpoint.ok) {
            console.error(endpoint)
            navi('/login')
            return
        }
        const socket = new SockJS(await endpoint.text())
        socket.onmessage = e => {
            onMsg(socket, JSON.parse(e.data), dispatch)
        }
        socket.onclose = e => {
            console.warn({
                warn: "socket closed",
                reason: e.reason,
                code: e.code
            })
            if (e.code < 4000) refresh()
            else navi('/login')
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
        }
        return () => {
            dispatch(applyDiff({'/socket': undefined}))
            socket.close()
        }
    }, [token, flag])
    return prop.children || (<></>)
}

function onMsg(socket: WebSocket, json: any, dispatch: AppDispatch) {
    switch (json['type']) {
        case 'auth.pass': {
            dispatch(applyDiff({'/socket': undefined}))
            break
        }
    }
}
