import { useEffect, useState } from "react"
import { ShieldCheck, Database, PersonStanding, Eye, EyeOff } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { useAuth } from "@/features/auth"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"

export function LoginPage() {
  const navigate = useNavigate()
  const {
    clearError,
    error,
    isAuthenticated,
    isLoading,
    signIn,
  } = useAuth()

  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)

  useEffect(() => {
    if (isAuthenticated) {
      navigate("/", { replace: true })
    }
  }, [isAuthenticated, navigate])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    clearError()
    void signIn(email, password)
  }

  return (
    <main className="flex min-h-screen w-full flex-col lg:flex-row bg-white dark:bg-[#101922]">
      {/* Left Side: Branding / Illustration */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between p-12 bg-[#137fec] relative overflow-hidden">
        {/* Decorative Background Pattern */}
        <div 
          className="absolute inset-0 opacity-10 pointer-events-none" 
          style={{ backgroundImage: 'radial-gradient(circle at 2px 2px, white 1px, transparent 0)', backgroundSize: '40px 40px' }}
        />
        
        <div className="relative z-10">
          <div className="flex items-center gap-3 text-white mb-12">
            <div className="size-10 bg-white/20 rounded-lg flex items-center justify-center backdrop-blur-sm">
              <Database className="text-white h-6 w-6" />
            </div>
            <h2 className="text-2xl font-bold tracking-tight">Pre-Sales Assets</h2>
          </div>
          
          <div className="max-w-md">
            <h1 className="text-5xl font-extrabold text-white leading-tight mb-6">
              Centralize your sales collateral.
            </h1>
            <p className="text-white/80 text-lg leading-relaxed">
              Access the latest decks, demos, and whitepapers in one secure location. Empower your team to close deals faster.
            </p>
          </div>
        </div>

        <div className="relative z-10 mt-auto">
          <div className="bg-white/10 backdrop-blur-md rounded-xl p-6 border border-white/20">
            <div className="flex gap-4 items-center">
              <div className="size-12 rounded-full overflow-hidden bg-white/20 flex items-center justify-center">
                <PersonStanding className="text-white h-6 w-6" />
              </div>
              <div>
                <p className="text-white font-semibold">"The single source of truth for our sales engineering team."</p>
                <p className="text-white/60 text-sm mt-1">Director of Solutions Architecture</p>
              </div>
            </div>
          </div>
        </div>

        {/* Abstract Graphic */}
        <div className="absolute -bottom-20 -right-20 size-80 bg-white/5 rounded-full blur-3xl pointer-events-none" />
      </div>

      {/* Right Side: Login Form */}
      <div className="flex-1 flex flex-col justify-center items-center px-6 py-12 lg:px-24 bg-white dark:bg-[#101922]">
        <div className="w-full max-w-[440px]">
          {/* Mobile Header */}
          <div className="lg:hidden flex items-center gap-3 mb-10">
            <div className="size-8 bg-[#137fec] rounded-lg flex items-center justify-center">
              <Database className="text-white h-5 w-5" />
            </div>
            <h2 className="text-xl font-bold text-slate-900 dark:text-white">Pre-Sales Assets</h2>
          </div>

          <div className="mb-10">
            <div className="mb-8 flex justify-center lg:justify-start">
               <img
                alt="Sedin logo"
                className="h-10 w-auto object-contain"
                src="/sedin-logo.png"
              />
            </div>
            <h2 className="text-3xl font-black text-slate-900 dark:text-white tracking-tight mb-2">Welcome Back</h2>
            <p className="text-slate-500 dark:text-slate-400">Please enter your details to sign in.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="flex flex-col gap-2">
              <Label htmlFor="email" className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Email Address
              </Label>
              <Input
                id="email"
                type="email"
                placeholder="name@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="h-12 border-slate-200 dark:border-slate-700 dark:bg-slate-800 focus:ring-primary/20"
              />
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="password" className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Password
              </Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className="h-12 border-slate-200 dark:border-slate-700 dark:bg-slate-800 focus:ring-primary/20 pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-primary transition-colors"
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Checkbox id="remember" className="border-slate-300 dark:border-slate-700 text-primary" />
                <Label htmlFor="remember" className="text-sm text-slate-600 dark:text-slate-400 cursor-pointer">
                  Remember me
                </Label>
              </div>
              <a href="#" className="text-sm font-semibold text-[#137fec] hover:underline underline-offset-4">
                Forgot Password?
              </a>
            </div>

            {error && (
              <div className="rounded-lg bg-red-50 dark:bg-red-900/20 p-4 text-sm font-medium text-red-600 dark:text-red-400 ring-1 ring-inset ring-red-100 dark:ring-red-900/30">
                {error}
              </div>
            )}

            <Button
              type="submit"
              disabled={isLoading}
              className="w-full bg-[#137fec] hover:bg-[#137fec]/90 text-white font-bold py-6 rounded-lg shadow-lg shadow-[#137fec]/20 transition-all active:scale-[0.98]"
            >
              {isLoading ? "Signing In..." : "Sign In"}
            </Button>

            <div className="relative flex items-center py-4">
              <div className="flex-grow border-t border-slate-200 dark:border-slate-700"></div>
              <span className="flex-shrink mx-4 text-xs font-medium text-slate-400 uppercase tracking-widest">Or continue with</span>
              <div className="flex-grow border-t border-slate-200 dark:border-slate-700"></div>
            </div>

            <Button
              variant="outline"
              type="button"
              className="w-full flex items-center justify-center gap-3 border-slate-200 dark:border-slate-700 py-6 hover:bg-slate-50 dark:hover:bg-slate-700/50"
            >
               <ShieldCheck className="h-5 w-5 text-[#137fec]" />
               SSO Provider
            </Button>
          </form>

          <div className="mt-12 flex items-center justify-center gap-6 text-sm text-slate-400 dark:text-slate-500">
            <a className="hover:text-[#137fec] transition-colors" href="#">Privacy Policy</a>
            <a className="hover:text-[#137fec] transition-colors" href="#">Terms of Service</a>
            <a className="hover:text-[#137fec] transition-colors" href="#">Help Center</a>
          </div>
        </div>
      </div>
    </main>
  )
}
