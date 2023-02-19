import './App.css'
import Loading from "./Loading"
import {Counter} from "./Counter"
import Login from "./Login"
import {Game} from "./Game"
import {setPage$} from "../dbus"
import {useObservable} from "../utils/react";

export default function App() {
    const page = useObservable(setPage$, 'load')
    switch (page) {
        case 'load': return <Loading/>
        case 'login': return <Login/>
        case 'game': return <Game/>
        default: return <Counter/>
    }
}
