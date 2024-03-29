import {useState} from "react"
import {useAppDispatch} from "../storeHook"
import {diffGame} from "../stores/gameState"
import {setPage$} from "../dbus";

async function login(server: string, name: string, pwd: string) {
    let url: URL
    try {
        url = new URL(server, window.location.href)
    } catch (e) {
        throw "not valid URL"
    }
    const res = await fetch(url, {
        method: 'post',
        headers: {
            name, pwd
        }
    })
    if (!res.ok) throw await res.text()
    return await res.text()
}

export default function Login() {
    const defaultServer = window.location.origin + window.location.pathname
    const [server, setServer] = useState('')
    const [name, setName] = useState('')
    const [pwd, setPwd] = useState('')
    const [buttonLock, setButtonLock] = useState(false)
    const [msg, setMsg] = useState('')
    const dispatch = useAppDispatch()
    return (<>
        <span style={{fontSize: '2rem', lineHeight: '3rem'}} className={"serif"}>Kita! Kita!</span>
        <input type='text' autoComplete='url' value={server}
                  onChange={e => setServer(e.target.value)} placeholder={`服务器(${defaultServer})`}/>
        <input type='text' autoComplete='username' value={name}
                  onChange={e => setName(e.target.value)} placeholder='用户名'/>
        <input type='password' value={pwd} onChange={e => setPwd(e.target.value)} placeholder='密码'/>
        {msg ? (<span style={{color: 'red'}}>{msg}</span>) : (<></>)}
        <button onClick={async e => {
            setButtonLock(true)
            try {
                const url = server || defaultServer
                const token = await login(url + '/login', name, pwd)
                dispatch(diffGame({
                    server: {
                        username: name,
                        token,
                        url
                    },
                    connection: {
                        state: 'connecting'
                    }
                }))
                setPage$.next('game')
            } catch (e) {
                setMsg(`[${new Date().toLocaleString()}] ${e}`)
            }
            setButtonLock(false)
        }} disabled={buttonLock}>登录</button>
    </>)
}
