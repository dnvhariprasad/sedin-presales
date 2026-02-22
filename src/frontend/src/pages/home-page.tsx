import { FolderOpen } from "lucide-react"

export function HomePage() {
  return (
    <div className="flex-1 flex flex-col items-center justify-center px-8 py-16">
      <div className="flex flex-col items-center text-center max-w-md">
        <div className="size-16 bg-slate-100 dark:bg-slate-800 rounded-2xl flex items-center justify-center mb-6">
          <FolderOpen className="h-8 w-8 text-slate-400" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-2">No assets yet</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Upload your first document to get started. Your pre-sales collateral and case studies will appear here.
        </p>
      </div>
    </div>
  )
}
