import './App.css'
import Loading from "./Loading"
import {Counter} from "./Counter";
import {createMemoryRouter, RouterProvider, useNavigate} from "react-router-dom"
import Login from "./Login"
import Socket from "./Socket";

const router = createMemoryRouter([
    {
        path: 'load',
        element: <Loading/>
    },
    {
        path: 'login',
        element: <Login/>
    },
    {
        path: 'game',
        element: <Socket></Socket>
    }
], {
    initialEntries: ["/load"]
})

export default function App() {
    return (<RouterProvider router={router} fallbackElement={<Counter/>}/>)
}
