import { useEffect, useRef } from 'react'
import { X } from '@phosphor-icons/react'

// <dialog> nativo: foco preso, Esc e backdrop de graça.
export default function Modal({ title, onClose, children }) {
  const ref = useRef(null)
  // Sem close() no cleanup: ele dispararia onClose e o StrictMode fecharia o modal ao montar.
  useEffect(() => {
    if (!ref.current.open) ref.current.showModal()
  }, [])
  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => e.target === ref.current && onClose()}
      aria-labelledby="modal-title"
      className="m-auto w-[calc(100%-2rem)] max-w-lg rounded-xl border border-line bg-surface p-0 text-ink shadow-2xl backdrop:bg-zinc-950/40"
    >
      <div className="p-5 md:p-6">
        <div className="mb-5 flex items-center justify-between">
          <h2 id="modal-title" className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="rounded-lg p-1.5 text-ink-2 hover:bg-line/50" aria-label="Close"><X size={18} /></button>
        </div>
        {children}
      </div>
    </dialog>
  )
}
