import { Outlet, useLocation, useNavigate } from "react-router-dom"
import {
  LogOut,
  Sparkles,
  Upload,
  LayoutGrid,
  Settings,
  FolderOpen,
  FileText,
} from "lucide-react"

import { useAuth } from "@/features/auth"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

const navItems = [
  { label: "All Assets", href: "/", icon: FolderOpen, section: "Library" },
  { label: "My Documents", href: "/my-documents", icon: FileText, section: "Library" },
  { label: "Masters Management", href: "/masters", icon: Settings, section: "System" },
]

export function AppShell() {
  const navigate = useNavigate()
  const location = useLocation()
  const { signOut, user } = useAuth()

  const sections = [...new Set(navItems.map((item) => item.section))]

  return (
    <div className="flex h-screen w-full flex-col bg-[#f8fafc] dark:bg-[#101922] font-sans">
      {/* Top Navigation Bar */}
      <header className="flex-none h-16 bg-white dark:bg-[#1e293b] border-b border-slate-200 dark:border-slate-700 flex items-center justify-between px-6 z-20">
        <div
          className="flex items-center gap-3 cursor-pointer"
          onClick={() => navigate("/")}
        >
          <div className="size-9 bg-[#137fec]/10 rounded-lg flex items-center justify-center text-[#137fec]">
            <LayoutGrid className="h-5 w-5" />
          </div>
          <h1 className="text-lg font-bold tracking-tight text-slate-900 dark:text-white">
            Asset Manager
          </h1>
        </div>

        <div className="flex-1 max-w-2xl mx-12 hidden md:block">
          <div className="relative group">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Sparkles className="h-4 w-4 text-[#137fec]" />
            </div>
            <Input
              className="block w-full rounded-lg border-0 py-2.5 pl-10 pr-4 text-slate-900 ring-1 ring-inset ring-slate-200 placeholder:text-slate-400 focus:ring-2 focus:ring-inset focus:ring-[#137fec] sm:text-sm sm:leading-6 bg-slate-50 dark:bg-slate-800 dark:text-white dark:ring-slate-700"
              placeholder="Ask AI to find case studies (e.g., 'Retail cloud migration success stories')..."
            />
            <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
              <span className="text-[10px] font-bold text-slate-400 border border-slate-200 rounded px-1.5 py-0.5">
                ⌘K
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <Button className="hidden sm:flex items-center gap-2 bg-[#137fec] hover:bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-colors shadow-sm shadow-blue-500/20 border-none">
            <Upload className="h-4 w-4" />
            Upload New
          </Button>

          <div className="h-6 w-px bg-slate-200 dark:bg-slate-700 mx-2" />

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="relative outline-none">
                <div className="size-9 rounded-full bg-slate-200 overflow-hidden ring-2 ring-white dark:ring-slate-800 flex items-center justify-center">
                  <div className="size-full bg-slate-300 dark:bg-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300 font-bold text-sm">
                    {user?.displayName?.charAt(0) ?? "U"}
                  </div>
                </div>
                <div className="absolute bottom-0 right-0 size-2.5 bg-green-500 border-2 border-white dark:border-slate-800 rounded-full" />
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium leading-none">
                    {user?.displayName}
                  </p>
                  <p className="text-xs leading-none text-slate-500">
                    {user?.email}
                  </p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              {user?.role === "ADMIN" && (
                <DropdownMenuItem
                  onClick={() => navigate("/masters")}
                  className="cursor-pointer"
                >
                  <Settings className="mr-2 h-4 w-4" />
                  <span>Admin Settings</span>
                </DropdownMenuItem>
              )}
              <DropdownMenuItem
                onClick={() => void signOut()}
                className="text-red-600 cursor-pointer"
              >
                <LogOut className="mr-2 h-4 w-4" />
                <span>Sign out</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar Navigation */}
        <aside className="w-64 flex-none bg-white dark:bg-[#1e293b] border-r border-slate-200 dark:border-slate-700 flex flex-col justify-between py-6 overflow-y-auto hidden md:flex">
          <div className="px-4 space-y-1">
            {sections.map((section) => (
              <div key={section}>
                <div className="px-2 mb-2 mt-4 first:mt-0 text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                  {section}
                </div>
                {navItems
                  .filter((item) => item.section === section)
                  .map((item) => {
                    const isActive = location.pathname === item.href
                    const Icon = item.icon
                    return (
                      <button
                        key={item.href}
                        onClick={() => navigate(item.href)}
                        className={`w-full group flex items-center gap-3 px-3 py-2 text-sm font-semibold rounded-lg transition-colors ${
                          isActive
                            ? "text-[#137fec] bg-blue-50 dark:bg-blue-900/20"
                            : "text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800"
                        }`}
                      >
                        <Icon
                          className={`h-5 w-5 ${
                            isActive
                              ? ""
                              : "text-slate-400 group-hover:text-slate-600"
                          }`}
                        />
                        {item.label}
                      </button>
                    )
                  })}
                {section !== sections[sections.length - 1] && (
                  <div className="my-4 border-t border-slate-100 dark:border-slate-800" />
                )}
              </div>
            ))}
          </div>

          <div className="px-6 mt-auto">
            <div className="bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-slate-800 dark:to-slate-900 rounded-xl p-4 border border-blue-100 dark:border-slate-700">
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] font-bold text-blue-800 dark:text-blue-300 uppercase tracking-tight">
                  Storage Usage
                </span>
                <span className="text-[10px] font-bold text-blue-800 dark:text-blue-300">
                  75%
                </span>
              </div>
              <div className="w-full bg-blue-200 dark:bg-slate-700 rounded-full h-1.5 mb-2">
                <div
                  className="bg-[#137fec] h-1.5 rounded-full"
                  style={{ width: "75%" }}
                />
              </div>
              <p className="text-[10px] text-slate-500 dark:text-slate-400">
                15GB of 20GB used
              </p>
            </div>
          </div>
        </aside>

        {/* Page Content */}
        <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
