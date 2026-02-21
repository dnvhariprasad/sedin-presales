import { useState } from "react"
import { 
  LogOut,
  Sparkles,
  Upload, 
  Filter, 
  Download, 
  X, 
  FileText, 
  BarChart2, 
  Code, 
  Package, 
  Contact, 
  MoreVertical, 
  ChevronLeft, 
  ChevronRight,
  ArrowDown,
  LayoutGrid
} from "lucide-react"

import { useAuth } from "@/features/auth"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

export function HomePage() {
  const { signOut, user } = useAuth()
  const [searchQuery, setSearchQuery] = useState("")

  const mockData = [
    {
      id: 1,
      title: "Cloud Migration Strategy 2024",
      domain: "Cloud Infra",
      industry: "Fintech",
      technologies: ["AWS", "Kubernetes"],
      customer: "Acme Corp",
      version: "v1.2",
      updatedAt: "2 days ago",
      icon: <FileText className="h-5 w-5" />,
      iconBg: "bg-blue-50 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400"
    },
    {
      id: 2,
      title: "Retail Omnichannel Analytics",
      domain: "Data & AI",
      industry: "Retail",
      technologies: ["Python", "Snowflake"],
      customer: "ShopRite",
      version: "v2.0",
      updatedAt: "5 days ago",
      icon: <BarChart2 className="h-5 w-5" />,
      iconBg: "bg-emerald-50 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400"
    },
    {
      id: 3,
      title: "Legacy System Modernization",
      domain: "App Dev",
      industry: "Banking",
      technologies: ["Java", "Spring"],
      customer: "Global Bank",
      version: "v1.0",
      updatedAt: "1 week ago",
      icon: <Code className="h-5 w-5" />,
      iconBg: "bg-purple-50 text-purple-600 dark:bg-purple-900/30 dark:text-purple-400"
    },
    {
      id: 4,
      title: "Supply Chain Optimization",
      domain: "Operations",
      industry: "Logistics",
      technologies: ["SAP", "Oracle"],
      customer: "FastShip",
      version: "v3.1",
      updatedAt: "2 weeks ago",
      icon: <Package className="h-5 w-5" />,
      iconBg: "bg-orange-50 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400"
    },
    {
      id: 5,
      title: "Customer 360 Platform",
      domain: "CRM",
      industry: "Insurance",
      technologies: ["Salesforce"],
      customer: "SafeGuard",
      version: "v1.5",
      updatedAt: "3 weeks ago",
      icon: <Contact className="h-5 w-5" />,
      iconBg: "bg-pink-50 text-pink-600 dark:bg-pink-900/30 dark:text-pink-400"
    }
  ]

  return (
    <div className="flex h-screen w-full flex-col bg-[#f8fafc] dark:bg-[#101922] font-sans">
      {/* Top Navigation Bar */}
      <header className="flex-none h-16 bg-white dark:bg-[#1e293b] border-b border-slate-200 dark:border-slate-700 flex items-center justify-between px-6 z-20">
        <div className="flex items-center gap-3">
          <div className="size-9 bg-[#137fec]/10 rounded-lg flex items-center justify-center text-[#137fec]">
            <LayoutGrid className="h-5 w-5" />
          </div>
          <h1 className="text-lg font-bold tracking-tight text-slate-900 dark:text-white">Asset Manager</h1>
        </div>

        <div className="flex-1 max-w-2xl mx-12 hidden md:block">
          <div className="relative group">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Sparkles className="h-4 w-4 text-[#137fec]" />
            </div>
            <Input 
              className="block w-full rounded-lg border-0 py-2.5 pl-10 pr-4 text-slate-900 ring-1 ring-inset ring-slate-200 placeholder:text-slate-400 focus:ring-2 focus:ring-inset focus:ring-[#137fec] sm:text-sm sm:leading-6 bg-slate-50 dark:bg-slate-800 dark:text-white dark:ring-slate-700" 
              placeholder="Ask AI to find case studies (e.g., 'Retail cloud migration success stories')..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
              <span className="text-[10px] font-bold text-slate-400 border border-slate-200 rounded px-1.5 py-0.5">⌘K</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <Button className="hidden sm:flex items-center gap-2 bg-[#137fec] hover:bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-colors shadow-sm shadow-blue-500/20 border-none">
            <Upload className="h-4 w-4" />
            Upload New
          </Button>

          <div className="h-6 w-px bg-slate-200 dark:bg-slate-700 mx-2"></div>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="relative outline-none">
                <div className="size-9 rounded-full bg-slate-200 overflow-hidden ring-2 ring-white dark:ring-slate-800 flex items-center justify-center">
                   <div className="size-full bg-slate-300 dark:bg-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300 font-bold text-sm">
                      {user?.displayName?.charAt(0) ?? "U"}
                   </div>
                </div>
                <div className="absolute bottom-0 right-0 size-2.5 bg-green-500 border-2 border-white dark:border-slate-800 rounded-full"></div>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium leading-none">{user?.displayName}</p>
                  <p className="text-xs leading-none text-slate-500">{user?.email}</p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => void signOut()} className="text-red-600 cursor-pointer">
                <LogOut className="mr-2 h-4 w-4" />
                <span>Sign out</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Page Header & Filters */}
        <div className="flex-none px-8 py-8 space-y-4">
          <div className="flex items-end justify-between">
            <div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-white">All Assets</h2>
              <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Manage and organize your pre-sales collateral and case studies.</p>
            </div>
            <div className="flex items-center gap-3">
              <Button variant="outline" className="flex items-center gap-2 px-3 py-2 bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 shadow-sm transition-all h-10">
                <Filter className="h-4 w-4" />
                Filters
              </Button>
              <Button variant="outline" className="flex items-center gap-2 px-3 py-2 bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 shadow-sm transition-all h-10">
                <Download className="h-4 w-4" />
                Export
              </Button>
            </div>
          </div>

          {/* Active Filters Bar */}
          <div className="flex flex-wrap items-center gap-2 pt-2">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mr-2">Active Filters:</span>
            <Badge variant="secondary" className="bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 px-3 py-1 gap-1.5 font-medium shadow-sm">
              Domain: Cloud
              <X className="h-3 w-3 cursor-pointer hover:text-red-500" />
            </Badge>
            <Badge variant="secondary" className="bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 px-3 py-1 gap-1.5 font-medium shadow-sm">
              Industry: Finance
              <X className="h-3 w-3 cursor-pointer hover:text-red-500" />
            </Badge>
            <button className="text-xs font-bold text-[#137fec] hover:text-blue-700 ml-2">Clear All</button>
          </div>
        </div>

        {/* Data Grid Area */}
        <div className="flex-1 px-8 pb-8 overflow-hidden">
          <div className="bg-white dark:bg-[#1e293b] border border-slate-200 dark:border-slate-700 rounded-xl shadow-sm h-full flex flex-col overflow-hidden">
            {/* Table Header */}
            <div className="overflow-x-auto flex-1">
              <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-700">
                <thead className="bg-slate-50 dark:bg-slate-800/50 sticky top-0 z-10">
                  <tr>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-wider w-[400px]">
                      <div className="flex items-center gap-1 cursor-pointer hover:text-slate-700 group">
                        Title
                        <ArrowDown className="h-3 w-3 opacity-0 group-hover:opacity-100 transition-opacity" />
                      </div>
                    </th>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-wider w-40">Domain</th>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-wider w-40">Industry</th>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-wider">Technology</th>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-wider w-48">Customer</th>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-wider w-24">Version</th>
                    <th className="relative px-6 py-4 w-16">
                      <span className="sr-only">Actions</span>
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-[#1e293b] divide-y divide-slate-200 dark:divide-slate-700">
                  {mockData.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors group cursor-pointer">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className={`flex-shrink-0 size-10 rounded-lg flex items-center justify-center ${item.iconBg}`}>
                            {item.icon}
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-semibold text-slate-900 dark:text-white group-hover:text-[#137fec] transition-colors">{item.title}</div>
                            <div className="text-[11px] text-slate-500">Updated {item.updatedAt}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <Badge variant="secondary" className="bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-300 font-medium border-none px-2.5 py-0.5 rounded-full text-[11px]">
                          {item.domain}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-slate-600 dark:text-slate-400 font-medium">{item.industry}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex flex-wrap gap-1.5">
                          {item.technologies.map((tech) => (
                            <Badge key={tech} variant="outline" className="bg-indigo-50/50 dark:bg-indigo-900/20 text-indigo-700 dark:text-indigo-300 border-indigo-100 dark:border-indigo-800 px-2 py-0 text-[10px] font-bold">
                              {tech}
                            </Badge>
                          ))}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          <div className="size-6 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-[10px] font-bold text-slate-600 dark:text-slate-400">
                            {item.customer.split(' ').map(n => n[0]).join('')}
                          </div>
                          <div className="text-sm text-slate-600 dark:text-slate-400 font-medium">{item.customer}</div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-[11px] text-slate-500 font-mono font-bold tracking-tight">
                        {item.version}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 outline-none">
                          <MoreVertical className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="bg-white dark:bg-[#1e293b] px-6 py-4 border-t border-slate-200 dark:border-slate-700 flex items-center justify-between">
              <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                <div>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Showing <span className="font-bold text-slate-900 dark:text-slate-200">1</span> to <span className="font-bold text-slate-900 dark:text-slate-200">5</span> of <span className="font-bold text-slate-900 dark:text-slate-200">97</span> results
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <nav aria-label="Pagination" className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                    <Button variant="outline" size="icon" className="h-8 w-8 rounded-l-md border-slate-300 dark:border-slate-600 text-slate-500">
                      <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <Button className="h-8 w-8 bg-blue-50 dark:bg-blue-900 border-[#137fec] text-[#137fec] rounded-none hover:bg-blue-100 dark:hover:bg-blue-800">1</Button>
                    <Button variant="outline" className="h-8 w-8 rounded-none border-slate-300 dark:border-slate-600 text-slate-500">2</Button>
                    <Button variant="outline" className="h-8 w-8 rounded-none border-slate-300 dark:border-slate-600 text-slate-500 hidden md:inline-flex">3</Button>
                    <span className="relative inline-flex items-center px-3 py-2 border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-xs font-medium text-slate-700 dark:text-slate-400">...</span>
                    <Button variant="outline" size="icon" className="h-8 w-8 rounded-r-md border-slate-300 dark:border-slate-600 text-slate-500">
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </nav>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
