import React, { useState, useEffect, useRef, Suspense, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { EffectComposer, Bloom, Vignette } from '@react-three/postprocessing';
import * as THREE from 'three';
import { 
  ShoppingBag, Sparkles, ShieldCheck, Truck, Store, ArrowRight, 
  ChevronDown, Zap, CheckCircle2, Award, Clock, ArrowUpRight, 
  Layers, Lock, Bot, Package
} from 'lucide-react';
import { gsap, ScrollTrigger, useGSAP } from '../lib/gsap';
import { AnimatedNumber, StaggerReveal } from './motion/MotionPrimitives';

/**
 * Check if WebGL context is available in the current browser/GPU environment
 */
function isWebGLAvailable() {
  if (typeof window === 'undefined') return false;
  try {
    const canvas = document.createElement('canvas');
    return Boolean(
      window.WebGLRenderingContext &&
      (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
    );
  } catch (e) {
    return false;
  }
}

/**
 * ErrorBoundary for safe 3D WebGL fallback
 */
class Intro3DErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.warn('Intro 3D Canvas ErrorBoundary caught error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback;
    }
    return this.props.children;
  }
}

/**
 * 2D Obsidian-and-Warm-Amber Fallback when WebGL is unsupported or errors
 */
function Intro2DFallback() {
  return (
    <div className="w-full h-full min-h-[460px] flex items-center justify-center p-6 select-none relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-slate-950 to-[#0A0F1D] opacity-95" />
      <div className="absolute w-80 h-80 rounded-full bg-orange-500/15 blur-3xl pointer-events-none" />
      <div className="relative z-10 max-w-sm w-full bg-slate-900/80 backdrop-blur-xl border border-orange-500/30 rounded-3xl p-6 shadow-2xl text-center space-y-4">
        <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-orange-500 to-amber-500 text-white flex items-center justify-center mx-auto shadow-lg shadow-orange-500/30">
          <ShoppingBag className="w-8 h-8 stroke-[2.5]" />
        </div>
        <div>
          <h3 className="text-lg font-black text-white">QuickShop Flagship 2026</h3>
          <p className="text-xs text-slate-400 mt-1">Hệ sinh thái thương mại điện tử đa phân hệ Microservices</p>
        </div>
        <div className="flex items-center justify-center gap-2 text-xs font-mono font-bold text-orange-400 bg-orange-500/10 border border-orange-500/20 py-2 rounded-xl">
          <Zap className="w-4 h-4 text-orange-400" />
          <span>Sẵn sàng phục vụ 24/7</span>
        </div>
      </div>
    </div>
  );
}

/**
 * 3D Procedural Hero Composition:
 * Floating obsidian-metallic commerce card + dual glowing amber rings + floating satellite nodes
 */
function HeroScene({ scrollProgress = 0 }) {
  const groupRef = useRef(null);
  const cardRef = useRef(null);
  const ring1Ref = useRef(null);
  const ring2Ref = useRef(null);
  const satellitesRef = useRef([]);

  // Clean disposal on unmount
  useEffect(() => {
    return () => {
      // Automatic garbage collection for geometries and materials
    };
  }, []);

  useFrame(({ clock, pointer }) => {
    const elapsed = clock.getElapsedTime();

    if (groupRef.current) {
      // Gentle floating levitation
      groupRef.current.position.y = Math.sin(elapsed * 0.8) * 0.15;

      // Cursor tracking with smooth inertia
      const targetRotX = -pointer.y * 0.35 + scrollProgress * 0.5;
      const targetRotY = pointer.x * 0.45 + scrollProgress * 1.2;

      groupRef.current.rotation.x = THREE.MathUtils.lerp(groupRef.current.rotation.x, targetRotX, 0.05);
      groupRef.current.rotation.y = THREE.MathUtils.lerp(groupRef.current.rotation.y, targetRotY, 0.05);
    }

    // Opposing ring orbital rotations
    if (ring1Ref.current) {
      ring1Ref.current.rotation.z = elapsed * 0.25;
      ring1Ref.current.rotation.x = Math.PI / 3 + Math.sin(elapsed * 0.3) * 0.1;
    }
    if (ring2Ref.current) {
      ring2Ref.current.rotation.z = -elapsed * 0.2;
      ring2Ref.current.rotation.y = -Math.PI / 4 + Math.cos(elapsed * 0.35) * 0.12;
    }

    // Satellite orbital bobbing
    satellitesRef.current.forEach((sat, i) => {
      if (sat) {
        const offset = i * (Math.PI / 2);
        sat.position.y = Math.sin(elapsed * 1.2 + offset) * 0.25;
      }
    });
  });

  return (
    <group ref={groupRef} position={[0, 0, 0]}>
      {/* 1. Main Floating Commerce Card: Dark Obsidian Metallic Body */}
      <mesh ref={cardRef} position={[0, 0, 0]}>
        <boxGeometry args={[2.0, 2.9, 0.22]} />
        <meshStandardMaterial
          color="#0F172A"
          roughness={0.18}
          metalness={0.88}
        />
      </mesh>

      {/* 2. Razor-Thin Glowing Edge Highlight (Selective Bloom Accent) */}
      <mesh position={[0, 0, 0]}>
        <boxGeometry args={[2.04, 2.94, 0.24]} />
        <meshStandardMaterial
          wireframe
          color="#EA580C"
          emissive="#F97316"
          emissiveIntensity={1.6}
          toneMapped={false}
        />
      </mesh>

      {/* 3. Outer Concentric Orbital Ring 1 (Warm Amber Emissive) */}
      <mesh ref={ring1Ref} rotation={[Math.PI / 3, 0.2, 0]}>
        <torusGeometry args={[2.5, 0.035, 16, 72]} />
        <meshStandardMaterial
          color="#EA580C"
          emissive="#F97316"
          emissiveIntensity={1.8}
          toneMapped={false}
        />
      </mesh>

      {/* 4. Outer Concentric Orbital Ring 2 (Deep Charcoal with Orange Rim) */}
      <mesh ref={ring2Ref} rotation={[-Math.PI / 4, -0.3, 0]}>
        <torusGeometry args={[3.1, 0.025, 16, 72]} />
        <meshStandardMaterial
          color="#EA580C"
          emissive="#F97316"
          emissiveIntensity={1.4}
          toneMapped={false}
        />
      </mesh>

      {/* 5. Four Floating Geometric Satellite Nodes (Representing Core Microservices) */}
      {/* Node A: Catalog Service (Amber) */}
      <mesh
        ref={(el) => (satellitesRef.current[0] = el)}
        position={[2.2, 1.2, 0.5]}
      >
        <octahedronGeometry args={[0.32, 0]} />
        <meshStandardMaterial
          color="#F97316"
          emissive="#F97316"
          emissiveIntensity={1.5}
          toneMapped={false}
        />
      </mesh>

      {/* Node B: Smart Logistics GHN/GHTK (Blue) */}
      <mesh
        ref={(el) => (satellitesRef.current[1] = el)}
        position={[-2.3, -1.0, 0.4]}
      >
        <dodecahedronGeometry args={[0.28, 0]} />
        <meshStandardMaterial
          color="#38BDF8"
          emissive="#0284C7"
          emissiveIntensity={1.2}
          toneMapped={false}
        />
      </mesh>

      {/* Node C: Escrow 72h Payout (Emerald) */}
      <mesh
        ref={(el) => (satellitesRef.current[2] = el)}
        position={[-1.8, 1.6, -0.5]}
      >
        <octahedronGeometry args={[0.26, 0]} />
        <meshStandardMaterial
          color="#10B981"
          emissive="#059669"
          emissiveIntensity={1.2}
          toneMapped={false}
        />
      </mesh>

      {/* Node D: AI Shopping Assistant (Violet / Amber) */}
      <mesh
        ref={(el) => (satellitesRef.current[3] = el)}
        position={[2.0, -1.5, -0.4]}
      >
        <icosahedronGeometry args={[0.25, 0]} />
        <meshStandardMaterial
          color="#F59E0B"
          emissive="#EA580C"
          emissiveIntensity={1.4}
          toneMapped={false}
        />
      </mesh>
    </group>
  );
}

/**
 * Main 3D Canvas Stage with Studio Rim Lighting & Post-Processing Bloom
 */
function IntroCanvas({ scrollProgress }) {
  return (
    <Canvas
      dpr={[1, 1.5]}
      gl={{
        antialias: true,
        powerPreference: 'high-performance',
        alpha: true,
      }}
      camera={{ position: [0, 0, 7.2], fov: 42 }}
      className="w-full h-full pointer-events-auto"
    >
      {/* Studio Lighting Setup */}
      <ambientLight intensity={0.4} color="#FFFFFF" />
      <directionalLight position={[6, 8, 5]} intensity={1.5} color="#FFD1A4" />
      <directionalLight position={[-6, -4, -4]} intensity={2.2} color="#F97316" />
      <pointLight position={[0, 0, 3]} intensity={1.8} color="#FF6B00" distance={10} />

      {/* 3D Scene */}
      <HeroScene scrollProgress={scrollProgress} />

      {/* Post-Processing Pipeline: Selective Bloom + Depth Vignette */}
      <EffectComposer multisampling={0}>
        <Bloom
          mipmapBlur
          luminanceThreshold={0.85}
          luminanceSmoothing={0.2}
          intensity={0.65}
        />
        <Vignette eskil={false} offset={0.2} darkness={0.5} />
      </EffectComposer>
    </Canvas>
  );
}

/**
 * Editorial IntroPage Component
 */
export default function IntroPage({ onNavigateToHome, onNavigateToLogin }) {
  const [scrollProgress, setScrollProgress] = useState(0);
  const [hasWebGL, setHasWebGL] = useState(true);
  const containerRef = useRef(null);
  const heroTextRef = useRef(null);
  const chaptersRef = useRef(null);

  // WebGL support verification
  useEffect(() => {
    setHasWebGL(isWebGLAvailable());
  }, []);

  // GSAP ScrollTrigger Integration with Lenis Smooth Scroll
  useGSAP(() => {
    // Scroll progress measurement for 3D rotation
    ScrollTrigger.create({
      trigger: containerRef.current,
      start: 'top top',
      end: 'bottom bottom',
      onUpdate: (self) => {
        setScrollProgress(self.progress);
      },
    });

    // Hero Text Entrance Reveal
    if (heroTextRef.current) {
      gsap.fromTo(
        heroTextRef.current.children,
        { opacity: 0, y: 32 },
        {
          opacity: 1,
          y: 0,
          duration: 0.8,
          stagger: 0.12,
          ease: 'expo.out',
          clearProps: 'transform',
        }
      );
    }

    // Scroll reveal for Story Chapters
    const chapterCards = gsap.utils.toArray('.story-chapter-card');
    chapterCards.forEach((card, idx) => {
      gsap.fromTo(
        card,
        { opacity: 0, y: 40 },
        {
          opacity: 1,
          y: 0,
          duration: 0.7,
          ease: 'power3.out',
          scrollTrigger: {
            trigger: card,
            start: 'top 85%',
            toggleActions: 'play none none reverse',
          },
        }
      );
    });
  }, { scope: containerRef });

  const handleScrollToChapters = () => {
    const el = document.getElementById('story-chapters');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div 
      ref={containerRef}
      className="min-h-screen bg-[#0A0F1D] text-slate-100 font-sans selection:bg-orange-500 selection:text-white relative overflow-hidden"
    >
      {/* Ambient Radial Spotlight Background Glow */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[600px] bg-gradient-to-b from-orange-500/15 via-amber-500/5 to-transparent blur-[120px] pointer-events-none z-0" />
      <div className="absolute top-[40%] right-[-10%] w-[600px] h-[600px] bg-blue-500/10 blur-[140px] pointer-events-none z-0" />

      {/* =========================================================================
          SECTION 1: HERO 3D STAGE & EDITORIAL HEADLINE
          ========================================================================= */}
      <section className="relative min-h-[92vh] sm:min-h-screen flex items-center z-10 px-4 sm:px-8 max-w-7xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center w-full py-12 lg:py-0">
          
          {/* Left Column: Editorial Content (7 cols) */}
          <div ref={heroTextRef} className="lg:col-span-7 space-y-6 z-20">
            {/* Brand Tag Pill */}
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-slate-900/90 border border-orange-500/30 backdrop-blur-md shadow-lg shadow-orange-500/10">
              <span className="w-2 h-2 rounded-full bg-orange-500 animate-ping" />
              <span className="text-[11px] font-mono font-bold tracking-wider text-orange-400 uppercase">
                QUICKSHOP ECOSYSTEM 2026 • FLAGSHIP COMMERCE
              </span>
            </div>

            {/* Main Headline */}
            <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-white leading-[1.08] font-sans">
              Định Nghĩa Lại <br />
              Trải Nghiệm{' '}
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-orange-400 via-amber-300 to-orange-500">
                Mua Sắm Trực Tuyến
              </span>
            </h1>

            {/* Editorial Description */}
            <p className="text-sm sm:text-base text-slate-300 leading-relaxed max-w-xl font-normal">
              Hệ sinh thái thương mại điện tử đa gian hàng xây dựng trên kiến trúc microservices hiệu năng cao, tích hợp logistics thông minh GHN/GHTK, đối soát tức thời qua Ví Escrow và trợ lý AI cá nhân hóa.
            </p>

            {/* Two-Way Navigation CTA Buttons */}
            <div className="flex flex-wrap items-center gap-4 pt-3">
              {/* Primary CTA: "Mua sắm" */}
              <button
                type="button"
                onClick={onNavigateToHome}
                className="btn-shimmer group relative px-8 py-3.5 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white font-bold text-sm sm:text-base rounded-2xl shadow-xl shadow-orange-500/25 active:scale-95 transition-all flex items-center gap-2.5 cursor-pointer"
              >
                <ShoppingBag className="w-5 h-5 text-white" />
                <span>Mua sắm ngay</span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </button>

              {/* Secondary CTA: "Khám phá tính năng" */}
              <button
                type="button"
                onClick={handleScrollToChapters}
                className="px-6 py-3.5 bg-slate-900/80 hover:bg-slate-800 text-slate-200 hover:text-white font-bold text-sm sm:text-base rounded-2xl border border-slate-700/80 backdrop-blur-md active:scale-95 transition-all flex items-center gap-2 cursor-pointer"
              >
                <span>Khám phá tính năng</span>
                <ChevronDown className="w-4 h-4 text-slate-400" />
              </button>
            </div>

            {/* Trust Badges */}
            <div className="pt-4 flex flex-wrap items-center gap-6 text-xs text-slate-400 border-t border-slate-800/80 font-medium">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>100% Mall Chính Hãng</span>
              </div>
              <div className="flex items-center gap-2">
                <Truck className="w-4 h-4 text-orange-400" />
                <span>Giao Toàn Quốc 24H</span>
              </div>
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-blue-400" />
                <span>Ví Escrow Bảo Vệ 72H</span>
              </div>
            </div>
          </div>

          {/* Right Column: Interactive 3D Showcase Stage (5 cols) */}
          <div className="lg:col-span-5 h-[420px] sm:h-[520px] lg:h-[600px] w-full relative flex items-center justify-center">
            {/* 3D Canvas Stage with ErrorBoundary */}
            <Intro3DErrorBoundary fallback={<Intro2DFallback />}>
              {hasWebGL ? (
                <Suspense fallback={<Intro2DFallback />}>
                  <IntroCanvas scrollProgress={scrollProgress} />
                </Suspense>
              ) : (
                <Intro2DFallback />
              )}
            </Intro3DErrorBoundary>

            {/* Floating Subtle 3D Helper Badge */}
            <div className="absolute bottom-4 right-4 bg-slate-950/70 border border-slate-800 text-[10px] font-mono text-slate-400 px-3 py-1 rounded-full backdrop-blur-md pointer-events-none select-none flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-orange-500 animate-pulse" />
              <span>Di chuột để tương tác 3D</span>
            </div>
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 2: SCROLL-CHOREOGRAPHED 3D STORYTELLING CHAPTERS
          ========================================================================= */}
      <section id="story-chapters" ref={chaptersRef} className="py-20 px-4 sm:px-8 max-w-7xl mx-auto space-y-16 relative z-10">
        
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/30 text-orange-400 text-xs font-mono font-bold uppercase tracking-wider">
            <Sparkles className="w-3.5 h-3.5" />
            KIẾN TRÚC & ĐỘT PHÁ
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Ba Cột Trụ Vận Hành Của QuickShop
          </h2>
          <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
            Kết hợp sức mạnh microservices, chuỗi cung ứng đồng bộ và trí tuệ nhân tạo để mang lại trải nghiệm thương mại điện tử liền mạch.
          </p>
        </div>

        {/* 3 Story Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
          {/* Chapter 1: Multi-vendor Microservices */}
          <div className="story-chapter-card bg-slate-900/60 backdrop-blur-xl border border-slate-800 hover:border-orange-500/50 rounded-3xl p-6 sm:p-8 space-y-4 transition-all duration-300 group hover:-translate-y-1 shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-orange-500/15 border border-orange-500/30 flex items-center justify-center text-orange-400 group-hover:scale-105 group-hover:bg-orange-500 group-hover:text-white transition-all">
              <Store className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <div className="text-xs font-mono font-bold text-orange-400">CHƯƠNG 01</div>
              <h3 className="text-lg font-bold text-white group-hover:text-orange-400 transition-colors">
                Hệ Sinh Thái Đa Gian Hàng
              </h3>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed">
              Phân hệ Catalog Service & Identity RBAC đa vai trò (Người mua, Người bán, Shipper, Quản trị). Kiểm duyệt KYC nghiêm ngặt, đảm bảo 100% hàng hóa chính hãng có hóa đơn và bảo hành điện tử.
            </p>
            <div className="pt-2 flex items-center gap-2 text-xs font-bold text-orange-400 group-hover:translate-x-1 transition-transform">
              <span>Khám phá các gian hàng Mall</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </div>
          </div>

          {/* Chapter 2: Smart Logistics & Escrow Protection */}
          <div className="story-chapter-card bg-slate-900/60 backdrop-blur-xl border border-slate-800 hover:border-blue-500/50 rounded-3xl p-6 sm:p-8 space-y-4 transition-all duration-300 group hover:-translate-y-1 shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-blue-500/15 border border-blue-500/30 flex items-center justify-center text-blue-400 group-hover:scale-105 group-hover:bg-blue-500 group-hover:text-white transition-all">
              <Truck className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <div className="text-xs font-mono font-bold text-blue-400">CHƯƠNG 02</div>
              <h3 className="text-lg font-bold text-white group-hover:text-blue-400 transition-colors">
                Vận Chuyển GHN & Bảo Vệ 72H
              </h3>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed">
              Tích hợp trực tiếp hệ thống bưu cục GHN Express, GHTK, Viettel Post; tự động định tuyến GPS tối ưu. Cơ chế Ký quỹ Escrow giữ tiền an toàn và chỉ tất toán khi người mua hài lòng sau 72 giờ.
            </p>
            <div className="pt-2 flex items-center gap-2 text-xs font-bold text-blue-400 group-hover:translate-x-1 transition-transform">
              <span>Chính sách giao hàng an toàn</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </div>
          </div>

          {/* Chapter 3: AI Shopping Assistant */}
          <div className="story-chapter-card bg-slate-900/60 backdrop-blur-xl border border-slate-800 hover:border-amber-500/50 rounded-3xl p-6 sm:p-8 space-y-4 transition-all duration-300 group hover:-translate-y-1 shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 group-hover:scale-105 group-hover:bg-amber-500 group-hover:text-white transition-all">
              <Bot className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <div className="text-xs font-mono font-bold text-amber-400">CHƯƠNG 03</div>
              <h3 className="text-lg font-bold text-white group-hover:text-amber-400 transition-colors">
                Trợ Lý Mua Sắm AI Đột Phá
              </h3>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed">
              Trí tuệ nhân tạo QuickShop AI phân tích sở thích, tìm kiếm thông minh bằng ngôn ngữ tự nhiên, so sánh giá đa gian hàng tức thì và gợi ý mã giảm giá tối ưu nhất cho từng đơn hàng.
            </p>
            <div className="pt-2 flex items-center gap-2 text-xs font-bold text-amber-400 group-hover:translate-x-1 transition-transform">
              <span>Trải nghiệm chat cùng AI</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </div>
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 3: MINIMALIST BENTO STATS & METRICS
          ========================================================================= */}
      <section className="py-16 px-4 sm:px-8 max-w-7xl mx-auto relative z-10">
        <StaggerReveal triggerKey={scrollProgress > 0.3} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          
          {/* Stat Card 1 */}
          <div className="p-6 rounded-3xl bg-slate-900/50 border border-slate-800/80 backdrop-blur-md space-y-2">
            <div className="text-3xl sm:text-4xl font-black font-mono text-orange-400 tracking-tight">
              <AnimatedNumber value={50} suffix="+" />
            </div>
            <div className="text-xs font-bold text-white">Thương Hiệu Mall Chính Hãng</div>
            <p className="text-[11px] text-slate-400">Đầy đủ tem kiểm định chất lượng và bảo hành điện tử chính ngạch.</p>
          </div>

          {/* Stat Card 2 */}
          <div className="p-6 rounded-3xl bg-slate-900/50 border border-slate-800/80 backdrop-blur-md space-y-2">
            <div className="text-3xl sm:text-4xl font-black font-mono text-blue-400 tracking-tight">
              <AnimatedNumber value={24} suffix="h" />
            </div>
            <div className="text-xs font-bold text-white">Giao Hàng Tiêu Chuẩn</div>
            <p className="text-[11px] text-slate-400">Đồng bộ kho hàng thông minh tại Hà Nội, Đà Nẵng và TP. Hồ Chí Minh.</p>
          </div>

          {/* Stat Card 3 */}
          <div className="p-6 rounded-3xl bg-slate-900/50 border border-slate-800/80 backdrop-blur-md space-y-2">
            <div className="text-3xl sm:text-4xl font-black font-mono text-emerald-400 tracking-tight">
              <AnimatedNumber value={72} suffix="h" />
            </div>
            <div className="text-xs font-bold text-white">Bảo Vệ Đổi Trả Miễn Phí</div>
            <p className="text-[11px] text-slate-400">100% đổi trả không lý do, hoàn tiền vào Ví tức thì khi có khiếu nại.</p>
          </div>

          {/* Stat Card 4 */}
          <div className="p-6 rounded-3xl bg-slate-900/50 border border-slate-800/80 backdrop-blur-md space-y-2">
            <div className="text-3xl sm:text-4xl font-black font-mono text-amber-400 tracking-tight">
              <AnimatedNumber value={99.8} suffix="%" />
            </div>
            <div className="text-xs font-bold text-white">Tỷ Lệ Đơn Hàng Hài Lòng</div>
            <p className="text-[11px] text-slate-400">Hơn 500.000+ lượt đánh giá 5 sao từ khách hàng thực tế trên sàn.</p>
          </div>

        </StaggerReveal>
      </section>

      {/* =========================================================================
          SECTION 4: HIGH-CONTRAST OBSIDIAN FINAL CTA BANNER
          ========================================================================= */}
      <section className="py-20 px-4 sm:px-8 max-w-7xl mx-auto relative z-10">
        <div className="relative rounded-3xl bg-gradient-to-br from-slate-900 via-slate-950 to-[#0A0F1D] border border-orange-500/40 p-8 sm:p-14 overflow-hidden shadow-2xl shadow-orange-500/10">
          
          {/* Subtle Ambient Radial Glow */}
          <div className="absolute right-0 top-0 translate-x-1/3 -translate-y-1/3 w-96 h-96 bg-orange-500/20 rounded-full blur-3xl pointer-events-none" />
          
          <div className="relative z-10 max-w-2xl space-y-5">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-orange-500/20 border border-orange-500/30 text-orange-400 text-xs font-mono font-bold uppercase">
              <Zap className="w-3.5 h-3.5" />
              SẴN SÀNG TRẢI NGHIỆM
            </div>

            <h2 className="text-3xl sm:text-5xl font-black text-white tracking-tight leading-tight">
              Nâng Tầm Mua Sắm Cùng QuickShop Ngay Hôm Nay
            </h2>

            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
              Khám phá ngay hàng ngàn deal sốc Flash Sale, bộ sưu tập sản phẩm công nghệ - thời trang cao cấp và nhận voucher freeship toàn quốc.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-4">
              <button
                type="button"
                onClick={onNavigateToHome}
                className="btn-shimmer px-8 py-3.5 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white font-bold text-sm sm:text-base rounded-2xl shadow-xl shadow-orange-500/30 active:scale-95 transition-all flex items-center gap-2 cursor-pointer"
              >
                <ShoppingBag className="w-5 h-5" />
                <span>Bắt đầu mua sắm</span>
                <ArrowRight className="w-4 h-4" />
              </button>

              <button
                type="button"
                onClick={onNavigateToLogin}
                className="px-6 py-3.5 bg-slate-900/80 hover:bg-slate-800 text-slate-200 hover:text-white font-bold text-sm sm:text-base rounded-2xl border border-slate-700 backdrop-blur-md active:scale-95 transition-all cursor-pointer"
              >
                Đăng nhập tài khoản
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Simple Editorial Footer */}
      <footer className="border-t border-slate-800/80 py-8 px-4 sm:px-8 text-center text-xs text-slate-500 relative z-10">
        <p>© 2026 QuickShop Platform. Nền tảng thương mại điện tử Microservices kiến trúc cao cấp.</p>
      </footer>
    </div>
  );
}
