import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {Drawable} from "../types/Drawable"
import {sendSocket$} from "../dbus"

type MsgMeta = { target: string, seq: number }
type DiffMsg<T> = MsgMeta & { diff: { [key: string]: T } }
type SetMsg<T> = MsgMeta & { data: { [key: string]: T } }
type SeqData<T> = { seq: number, data: { [key: string]: T } }

function genSeq<T>(): SeqData<T> {
    return {
        seq: 0,
        data: {}
    }
}

export interface SeqState<T> { [key: string]: SeqData<T> }

export const seqStateSlicer = createSlice({
    name: 'seqState',
    initialState: {
        starDrawables: genSeq<Drawable>()
    } as SeqState<Drawable>,
    reducers: {
        diffSeq(state, action: PayloadAction<MsgMeta>) {
            if (!state[action.payload.target]) {
                state[action.payload.target] = genSeq<Drawable>()
            }
            const data = state[action.payload.target], diff = action.payload as DiffMsg<Drawable>
            if (data.seq >= diff.seq) return // equiv this.seq + 1 > diff.seq
            if (data.seq + 1 < diff.seq) {
                data.seq = 2 << 31
                sendSocket$.next({ type: "state.seq.require", target: diff.target })
                return
            }
            // this.seq + 1 == diff.seq
            data.seq = diff.seq
            for (const k in diff.diff) {
                if (data.data[k]) {
                    if (diff.diff[k] === null)
                        delete data.data[k]
                    else console.error("seq-diff: duplicate drawables")
                    continue
                }
                data.data[k] = diff.diff[k]
            }
        },
        setSeq(state, action: PayloadAction<MsgMeta>) {
            const data = state[action.payload.target], msg = action.payload as SetMsg<Drawable>
            data.seq = msg.seq
            data.data = msg.data
        }
    }
})

export const {diffSeq, setSeq} = seqStateSlicer.actions
