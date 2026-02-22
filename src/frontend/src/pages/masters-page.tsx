import { useState, useEffect } from "react"
import {
  Search,
  Plus,
  Edit2,
  Trash2,
  Grid,
  Database,
  Factory,
  Code,
  Settings,
  LayoutGrid,
  MoreVertical,
  CheckCircle2,
  AlertCircle
} from "lucide-react"
import { toast } from "sonner"

import { useAuth } from "@/features/auth"
import { api } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

interface MasterItem {
  id: string
  name: string
  description: string
  createdAt: string
}

interface MasterCategory {
  id: string
  name: string
  icon: React.ReactNode
  type: string
}

export function MastersPage() {
  const { user } = useAuth()
  const [activeCategory, setActiveCategory] = useState("domains")
  const [items, setItems] = useState<MasterItem[]>([])
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [catSearch, setCatSearch] = useState("")

  // Dialog states
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isEditOpen, setIsEditOpen] = useState(false)
  const [isDeleteOpen, setIsDeleteOpen] = useState(false)
  const [currentItem, setCurrentItem] = useState<MasterItem | null>(null)
  const [formData, setFormData] = useState({ name: "", description: "" })
  const [actionLoading, setActionLoading] = useState(false)

  const categories: MasterCategory[] = [
    { id: "domains", name: "Domain", icon: <Database className="h-4 w-4" />, type: "domains" },
    { id: "industries", name: "Industry", icon: <Factory className="h-4 w-4" />, type: "industries" },
    { id: "technologies", name: "Technology", icon: <Code className="h-4 w-4" />, type: "technologies" },
    { id: "document-types", name: "Asset Type", icon: <Grid className="h-4 w-4" />, type: "document-types" },
    { id: "business-units", name: "Business Unit", icon: <LayoutGrid className="h-4 w-4" />, type: "business-units" },
    { id: "sbus", name: "SBU", icon: <Settings className="h-4 w-4" />, type: "sbus" },
  ]

  useEffect(() => {
    fetchItems()
  }, [activeCategory])

  const fetchItems = async () => {
    setLoading(true)
    try {
      const response = await api.get(`/masters/${activeCategory}`)
      setItems(response.data.data.content)
    } catch (error) {
      console.error("Failed to fetch master items", error)
      toast.error("Failed to load items")
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setActionLoading(true)
    try {
      await api.post(`/masters/${activeCategory}`, formData)
      toast.success("Item created successfully")
      setIsCreateOpen(false)
      setFormData({ name: "", description: "" })
      fetchItems()
    } catch (error) {
      console.error("Failed to create item", error)
      toast.error("Failed to create item")
    } finally {
      setActionLoading(false)
    }
  }

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!currentItem) return

    setActionLoading(true)
    try {
      await api.put(`/masters/${activeCategory}/${currentItem.id}`, formData)
      toast.success("Item updated successfully")
      setIsEditOpen(false)
      setCurrentItem(null)
      setFormData({ name: "", description: "" })
      fetchItems()
    } catch (error) {
      console.error("Failed to update item", error)
      toast.error("Failed to update item")
    } finally {
      setActionLoading(false)
    }
  }

  const handleDelete = async () => {
    if (!currentItem) return

    setActionLoading(true)
    try {
      await api.delete(`/masters/${activeCategory}/${currentItem.id}`)
      toast.success("Item deleted successfully")
      setIsDeleteOpen(false)
      setCurrentItem(null)
      fetchItems()
    } catch (error) {
      console.error("Failed to delete item", error)
      toast.error("Failed to delete item")
    } finally {
      setActionLoading(false)
    }
  }

  const openEditDialog = (item: MasterItem) => {
    setCurrentItem(item)
    setFormData({ name: item.name, description: item.description || "" })
    setIsEditOpen(true)
  }

  const openDeleteDialog = (item: MasterItem) => {
    setCurrentItem(item)
    setIsDeleteOpen(true)
  }

  const filteredCategories = categories.filter(c =>
    c.name.toLowerCase().includes(catSearch.toLowerCase())
  )

  const filteredItems = items.filter(item =>
    item.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (item.description && item.description.toLowerCase().includes(searchQuery.toLowerCase()))
  )

  const currentCategoryName = categories.find(c => c.id === activeCategory)?.name

  return (
    <>
      <div className="flex flex-1 overflow-hidden">
        {/* Page-specific Categories Sidebar */}
        <aside className="w-72 flex-none bg-white dark:bg-[#1e293b]/50 border-r border-slate-200 dark:border-slate-700 flex flex-col overflow-hidden">
          <div className="p-4 border-b border-slate-200 dark:border-slate-700">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                className="pl-9 h-9 text-xs"
                placeholder="Filter categories..."
                value={catSearch}
                onChange={(e) => setCatSearch(e.target.value)}
              />
            </div>
          </div>

          <div className="flex-1 overflow-y-auto p-3 space-y-1">
            <p className="px-3 py-2 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Master Lists</p>
            {filteredCategories.map((cat) => (
              <button
                key={cat.id}
                onClick={() => setActiveCategory(cat.id)}
                className={`w-full text-left px-3 py-2.5 rounded-lg text-sm font-semibold flex items-center justify-between group transition-all ${
                  activeCategory === cat.id
                  ? "bg-[#137fec]/10 text-[#137fec]"
                  : "text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800"
                }`}
              >
                <span className="flex items-center gap-3">
                  {cat.icon}
                  {cat.name}
                </span>
              </button>
            ))}
          </div>
        </aside>

        {/* Main Content Area */}
        <div className="flex-1 flex flex-col min-w-0 bg-[#f8fafc] dark:bg-[#101922] p-8 overflow-hidden">
          <div className="mb-6">
            <h2 className="text-2xl font-bold text-slate-900 dark:text-white">Masters Management</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400">Configure global categories and lookup values.</p>
          </div>

          <div className="flex-1 flex flex-col bg-white dark:bg-[#1e293b] border border-slate-200 dark:border-slate-700 rounded-xl shadow-sm overflow-hidden">
            {/* Table Header Controls */}
            <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-700 flex items-center justify-between bg-slate-50/30 dark:bg-slate-800/20">
              <div className="flex items-center gap-4">
                <h3 className="text-base font-bold text-slate-900 dark:text-white">{currentCategoryName} Values</h3>
                <Badge variant="secondary" className="bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-none font-bold text-[10px]">
                  {items.length} Entries
                </Badge>
              </div>

              <div className="flex items-center gap-3">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <Input
                    className="pl-9 h-10 w-64 bg-white dark:bg-slate-900"
                    placeholder="Search values..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </div>
                {user?.role === "ADMIN" && (
                  <Button
                    onClick={() => {
                      setFormData({ name: "", description: "" })
                      setIsCreateOpen(true)
                    }}
                    className="bg-[#137fec] hover:bg-blue-600 text-white gap-2 font-bold px-4 h-10 border-none"
                  >
                    <Plus className="h-4 w-4" />
                    Add New Value
                  </Button>
                )}
              </div>
            </div>

            {/* Table */}
            <div className="flex-1 overflow-auto">
              <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-700">
                <thead className="bg-slate-50/50 dark:bg-slate-800/50 sticky top-0 z-10">
                  <tr>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-widest">Value Name</th>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-widest">Description</th>
                    <th className="px-6 py-4 text-left text-[11px] font-bold text-slate-500 uppercase tracking-widest">Status</th>
                    {user?.role === "ADMIN" && (
                      <th className="px-6 py-4 text-right text-[11px] font-bold text-slate-500 uppercase tracking-widest">Actions</th>
                    )}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
                  {loading ? (
                    <tr>
                      <td colSpan={user?.role === "ADMIN" ? 4 : 3} className="px-6 py-12 text-center text-slate-500">
                        <div className="flex flex-col items-center gap-2">
                          <div className="h-6 w-6 border-2 border-[#137fec] border-t-transparent rounded-full animate-spin"></div>
                          <span className="text-xs font-bold uppercase tracking-widest">Loading values...</span>
                        </div>
                      </td>
                    </tr>
                  ) : filteredItems.length > 0 ? (
                    filteredItems.map((item) => (
                      <tr key={item.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors group">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-bold text-slate-900 dark:text-white">{item.name}</div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-sm text-slate-500 dark:text-slate-400 line-clamp-1">
                            {item.description || "No description provided"}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <Badge className="bg-emerald-50 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-400 border-emerald-100 dark:border-emerald-800 gap-1.5 font-bold text-[10px] uppercase px-2.5">
                            <CheckCircle2 className="h-3 w-3" />
                            Active
                          </Badge>
                        </td>
                        {user?.role === "ADMIN" && (
                          <td className="px-6 py-4 whitespace-nowrap text-right">
                            <div className="flex justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => openEditDialog(item)}
                                className="h-8 w-8 text-slate-400 hover:text-[#137fec]"
                              >
                                <Edit2 className="h-4 w-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => openDeleteDialog(item)}
                                className="h-8 w-8 text-slate-400 hover:text-red-500"
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>
                            <div className="group-hover:hidden">
                              <MoreVertical className="h-4 w-4 text-slate-300 ml-auto" />
                            </div>
                          </td>
                        )}
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={user?.role === "ADMIN" ? 4 : 3} className="px-6 py-12 text-center text-slate-500">
                        No values found for this category.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* Create Dialog */}
      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add New {currentCategoryName}</DialogTitle>
            <DialogDescription>
              Create a new entry in the {currentCategoryName} master list.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreate}>
            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="name">Name</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder={`Enter ${currentCategoryName?.toLowerCase()} name`}
                  required
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Enter a brief description (optional)"
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={actionLoading}>
                {actionLoading ? "Creating..." : "Create Value"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={isEditOpen} onOpenChange={setIsEditOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit {currentCategoryName}</DialogTitle>
            <DialogDescription>
              Update the details for this {currentCategoryName?.toLowerCase()}.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleUpdate}>
            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="edit-name">Name</Label>
                <Input
                  id="edit-name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-description">Description</Label>
                <Textarea
                  id="edit-description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsEditOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={actionLoading}>
                {actionLoading ? "Saving..." : "Save Changes"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={isDeleteOpen} onOpenChange={setIsDeleteOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-red-600">
              <AlertCircle className="h-5 w-5" />
              Confirm Deletion
            </DialogTitle>
            <DialogDescription>
              Are you sure you want to delete <strong>{currentItem?.name}</strong>? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button type="button" variant="outline" onClick={() => setIsDeleteOpen(false)}>
              Cancel
            </Button>
            <Button
              type="button"
              variant="destructive"
              onClick={handleDelete}
              disabled={actionLoading}
            >
              {actionLoading ? "Deleting..." : "Delete Value"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
