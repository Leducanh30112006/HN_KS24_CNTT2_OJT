import React, { useState, useEffect, useRef, Suspense, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { EffectComposer, Bloom, Vignette } from '@react-three/postprocessing';
import * as THREE from 'three';
import { 
  ShoppingBag, Sparkles, ShieldCheck, Truck, Store, ArrowRight, 
  ChevronDown, Zap, CheckCircle2, Award, Clock, ArrowUpRight, 
  Layers, Lock, Bot, Package, UserCheck, Shield, ChevronRight, 
  RefreshCw, Smartphone, CreditCard, Search, Check, QrCode, Cpu,
  Database, Activity, ArrowDown, ExternalLink
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
 * Ultra-lightweight native procedural particle field (warm amber dust)
 */
function AmberParticles({ count = 65 }) {
  const pointsRef = useRef();
  const [positions] = useMemo(() => {
    const pos = new Float32Array(count * 3);
    for (let i = 0; i < count; i++) {
      pos[i * 3] = (Math.random() - 0.5) * 11;
      pos[i * 3 + 1] = (Math.random() - 0.5) * 9;
      pos[i * 3 + 2] = (Math.random() - 0.5) * 7;
    }
    return [pos];
  }, [count]);

  useFrame(({ clock }) => {
    if (pointsRef.current) {
      pointsRef.current.rotation.y = clock.getElapsedTime() * 0.04;
      pointsRef.current.rotation.x = Math.sin(clock.getElapsedTime() * 0.03) * 0.08;
    }
  });

  return (
    <points ref={pointsRef}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          count={count}
          array={positions}
          itemSize={3}
        />
      </bufferGeometry>
      <pointsMaterial
        size={0.065}
        color="#F97316"
        transparent
        opacity={0.6}
        blending={THREE.AdditiveBlending}
        sizeAttenuation
      />
    </points>
  );
}

/**
 * Multi-Part Kinetic Assembly:
 * - Inner Obsidian Core with bevel
 * - Floating Frosted-Glass Commerce Cards
 * - 3 Concentric Orbital Rings with selective #F97316 emissive edges
 * - 4 Satellite Microservice Nodes that react dynamically to chapter transitions
 */
function KineticAssembly({ activeChapter = 0, scrollProgress = 0 }) {
  const groupRef = useRef(null);
  const coreRef = useRef(null);
  const card1Ref = useRef(null);
  const card2Ref = useRef(null);
  const ring1Ref = useRef(null);
  const ring2Ref = useRef(null);
  const ring3Ref = useRef(null);
  const satellitesRef = useRef([]);

  useFrame(({ clock, pointer }) => {
    const elapsed = clock.getElapsedTime();

    if (groupRef.current) {
      // 1. Natural levitation
      groupRef.current.position.y = Math.sin(elapsed * 0.8) * 0.12;

      // 2. Cursor tracking parallax with smooth lerp
      const targetRotX = -pointer.y * 0.35 + scrollProgress * 0.6;
      const targetRotY = pointer.x * 0.45 + scrollProgress * 1.5;

      groupRef.current.rotation.x = THREE.MathUtils.lerp(groupRef.current.rotation.x, targetRotX, 0.05);
      groupRef.current.rotation.y = THREE.MathUtils.lerp(groupRef.current.rotation.y, targetRotY, 0.05);
    }

    // 3. Inner Obsidian Core Rotation
    if (coreRef.current) {
      let coreRotSpeed = 0.25;
      let coreScale = 1.0;
      if (activeChapter === 4) {
        // AI Concierge chapter: core spins faster and pulses
        coreRotSpeed = 1.2;
        coreScale = 1.15 + Math.sin(elapsed * 3.5) * 0.08;
      } else if (activeChapter === 3) {
        // Escrow chapter: core forms a locked geometric shield orientation
        coreRotSpeed = 0.08;
        coreScale = 1.05;
      }
      coreRef.current.rotation.y += coreRotSpeed * 0.016;
      coreRef.current.rotation.z = Math.sin(elapsed * 0.5) * 0.15;
      coreRef.current.scale.setScalar(THREE.MathUtils.lerp(coreRef.current.scale.x, coreScale, 0.08));
    }

    // 4. Frosted Glass Cards (separation across delivery tiers in chapter 2)
    if (card1Ref.current && card2Ref.current) {
      let card1TargetY = 0;
      let card2TargetY = 0;
      let card2TargetZ = -0.3;

      if (activeChapter === 2) {
        // Logistics chapter: Cards separate into Express 2H vs Standard 24H tiers
        card1TargetY = 0.65;
        card2TargetY = -0.65;
        card2TargetZ = 0.35;
      }

      card1Ref.current.position.y = THREE.MathUtils.lerp(card1Ref.current.position.y, card1TargetY, 0.07);
      card2Ref.current.position.y = THREE.MathUtils.lerp(card2Ref.current.position.y, card2TargetY, 0.07);
      card2Ref.current.position.z = THREE.MathUtils.lerp(card2Ref.current.position.z, card2TargetZ, 0.07);

      card1Ref.current.rotation.z = Math.sin(elapsed * 0.6) * 0.05;
      card2Ref.current.rotation.z = -Math.sin(elapsed * 0.6) * 0.05;
    }

    // 5. 3 Concentric Orbital Rings
    let ringScale = 1.0;
    if (activeChapter === 1) {
      // Multi-vendor chapter: rings expand outward
      ringScale = 1.25;
    } else if (activeChapter === 4) {
      // AI Concierge: rings compress inward
      ringScale = 0.85;
    }

    if (ring1Ref.current) {
      ring1Ref.current.rotation.z = elapsed * 0.28;
      ring1Ref.current.rotation.x = Math.PI / 3 + Math.sin(elapsed * 0.4) * 0.1;
      ring1Ref.current.scale.setScalar(THREE.MathUtils.lerp(ring1Ref.current.scale.x, ringScale, 0.06));
    }
    if (ring2Ref.current) {
      ring2Ref.current.rotation.z = -elapsed * 0.22;
      ring2Ref.current.rotation.y = -Math.PI / 4 + Math.cos(elapsed * 0.35) * 0.12;
      ring2Ref.current.scale.setScalar(THREE.MathUtils.lerp(ring2Ref.current.scale.x, ringScale * 1.05, 0.06));
    }
    if (ring3Ref.current) {
      ring3Ref.current.rotation.x = Math.PI / 6 + elapsed * 0.18;
      ring3Ref.current.rotation.y = Math.PI / 3 - Math.sin(elapsed * 0.3) * 0.15;
      ring3Ref.current.scale.setScalar(THREE.MathUtils.lerp(ring3Ref.current.scale.x, ringScale * 0.95, 0.06));
    }

    // 6. Four Satellite Microservice Nodes (Catalog, Logistics, Escrow, AI)
    satellitesRef.current.forEach((sat, i) => {
      if (sat) {
        const angle = elapsed * 0.5 + i * (Math.PI / 2);
        let radius = 2.4;
        if (activeChapter === 1) radius = 3.2; // Wide orbit in multi-vendor
        if (activeChapter === 4) radius = 1.6; // Converged orbit in AI

        const targetX = Math.cos(angle) * radius;
        const targetZ = Math.sin(angle) * radius;
        const targetY = Math.sin(elapsed * 1.4 + i) * 0.35;

        sat.position.x = THREE.MathUtils.lerp(sat.position.x, targetX, 0.05);
        sat.position.z = THREE.MathUtils.lerp(sat.position.z, targetZ, 0.05);
        sat.position.y = THREE.MathUtils.lerp(sat.position.y, targetY, 0.05);
        sat.rotation.x += 0.02;
        sat.rotation.y += 0.02;
      }
    });
  });

  return (
    <group ref={groupRef} position={[0, 0, 0]}>
      {/* 1. Ambient Warm Amber Particles */}
      <AmberParticles count={60} />

      {/* 2. Inner Obsidian Core (Faceted Metallic Centerpiece) */}
      <group ref={coreRef}>
        <mesh position={[0, 0, 0]}>
          <octahedronGeometry args={[0.92, 0]} />
          <meshStandardMaterial
            color="#0F172A"
            roughness={0.16}
            metalness={0.92}
          />
        </mesh>
        {/* Core Glowing Wireframe Accent */}
        <mesh position={[0, 0, 0]}>
          <octahedronGeometry args={[0.94, 0]} />
          <meshStandardMaterial
            wireframe
            color="#F97316"
            emissive="#F97316"
            emissiveIntensity={1.8}
            toneMapped={false}
          />
        </mesh>
      </group>

      {/* 3. Floating Frosted-Glass Commerce Cards */}
      <group ref={card1Ref} position={[0, 0, 0.15]}>
        <mesh>
          <boxGeometry args={[1.9, 2.7, 0.08]} />
          <meshStandardMaterial
            color="#1E293B"
            roughness={0.22}
            metalness={0.7}
            transparent
            opacity={0.85}
          />
        </mesh>
        {/* Glowing Orange Rim */}
        <mesh>
          <boxGeometry args={[1.94, 2.74, 0.09]} />
          <meshStandardMaterial
            wireframe
            color="#EA580C"
            emissive="#F97316"
            emissiveIntensity={1.5}
            toneMapped={false}
          />
        </mesh>
      </group>

      <group ref={card2Ref} position={[0.2, 0, -0.2]}>
        <mesh>
          <boxGeometry args={[1.6, 2.3, 0.06]} />
          <meshStandardMaterial
            color="#0F172A"
            roughness={0.3}
            metalness={0.8}
            transparent
            opacity={0.75}
          />
        </mesh>
      </group>

      {/* 4. Three Concentric Orbital Rings */}
      {/* Ring 1: Primary Warm Amber Glowing Rim */}
      <mesh ref={ring1Ref} rotation={[Math.PI / 3, 0.2, 0]}>
        <torusGeometry args={[2.3, 0.032, 16, 72]} />
        <meshStandardMaterial
          color="#EA580C"
          emissive="#F97316"
          emissiveIntensity={1.9}
          toneMapped={false}
        />
      </mesh>

      {/* Ring 2: Secondary Deep Charcoal & Amber Ring */}
      <mesh ref={ring2Ref} rotation={[-Math.PI / 4, -0.3, 0]}>
        <torusGeometry args={[2.9, 0.024, 16, 72]} />
        <meshStandardMaterial
          color="#EA580C"
          emissive="#F97316"
          emissiveIntensity={1.4}
          toneMapped={false}
        />
      </mesh>

      {/* Ring 3: Outer Horizon Ring */}
      <mesh ref={ring3Ref} rotation={[Math.PI / 6, 0.5, 0]}>
        <torusGeometry args={[3.5, 0.018, 16, 72]} />
        <meshStandardMaterial
          color="#38BDF8"
          emissive="#0284C7"
          emissiveIntensity={1.2}
          toneMapped={false}
        />
      </mesh>

      {/* 5. Four Satellite Microservice Nodes */}
      {/* Node 0: Multi-vendor Catalog Service */}
      <mesh ref={(el) => (satellitesRef.current[0] = el)} position={[2.4, 0, 0]}>
        <octahedronGeometry args={[0.28, 0]} />
        <meshStandardMaterial
          color="#F97316"
          emissive="#F97316"
          emissiveIntensity={1.7}
          toneMapped={false}
        />
      </mesh>

      {/* Node 1: Smart Logistics GHN/GHTK */}
      <mesh ref={(el) => (satellitesRef.current[1] = el)} position={[0, 0, 2.4]}>
        <dodecahedronGeometry args={[0.26, 0]} />
        <meshStandardMaterial
          color="#38BDF8"
          emissive="#0284C7"
          emissiveIntensity={1.4}
          toneMapped={false}
        />
      </mesh>

      {/* Node 2: Escrow 72H Protected Payout */}
      <mesh ref={(el) => (satellitesRef.current[2] = el)} position={[-2.4, 0, 0]}>
        <octahedronGeometry args={[0.26, 0]} />
        <meshStandardMaterial
          color="#10B981"
          emissive="#059669"
          emissiveIntensity={1.5}
          toneMapped={false}
        />
      </mesh>

      {/* Node 3: AI Shopping Assistant */}
      <mesh ref={(el) => (satellitesRef.current[3] = el)} position={[0, 0, -2.4]}>
        <icosahedronGeometry args={[0.25, 0]} />
        <meshStandardMaterial
          color="#F59E0B"
          emissive="#EA580C"
          emissiveIntensity={1.6}
          toneMapped={false}
        />
      </mesh>
    </group>
  );
}

/**
 * Single Main 3D Canvas Stage with Studio Rim Lighting & Post-Processing Bloom
 */
function SingleIntroCanvas({ activeChapter, scrollProgress }) {
  return (
    <Canvas
      dpr={[1, 1.5]}
      gl={{
        antialias: true,
        powerPreference: 'default',
        alpha: true,
      }}
      onCreated={({ gl }) => {
        if (gl?.domElement) {
          gl.domElement.addEventListener(
            'webglcontextlost',
            (e) => {
              e.preventDefault();
            },
            false
          );
        }
      }}
      camera={{ position: [0, 0, 7.2], fov: 42 }}
      className="w-full h-full pointer-events-auto"
    >
      {/* Studio Rim Lighting Setup */}
      <ambientLight intensity={0.45} color="#FFFFFF" />
      <directionalLight position={[6, 8, 5]} intensity={1.6} color="#FFD1A4" />
      <directionalLight position={[-6, -4, -4]} intensity={2.2} color="#F97316" />
      <pointLight position={[0, 0, 3.2]} intensity={2.0} color="#FF6B00" distance={10} />

      {/* Kinetic Assembly */}
      <KineticAssembly activeChapter={activeChapter} scrollProgress={scrollProgress} />

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
 * Main Flagship IntroPage Component with 6 Rich Sections
 */
export default function IntroPage({ 
  onNavigateToHome, 
  onNavigateToLogin,
  onNavigateToRegisterSeller,
  onNavigateToRegisterShipper
}) {
  const [scrollProgress, setScrollProgress] = useState(0);
  const [activeChapter, setActiveChapter] = useState(0); // 0 (Hero) to 4 (AI)
  const [activeRole, setActiveRole] = useState('buyer'); // 'buyer' | 'seller' | 'admin'
  const [activeHudSection, setActiveHudSection] = useState('01');
  const [hasWebGL, setHasWebGL] = useState(true);

  const containerRef = useRef(null);
  const heroTextRef = useRef(null);
  const scrollyContainerRef = useRef(null);
  const tiltCardRef = useRef(null);

  // WebGL support verification
  useEffect(() => {
    setHasWebGL(isWebGLAvailable());
  }, []);

  // GSAP Scrollytelling & Section Tracking Choreography
  useGSAP(() => {
    // 1. Overall Page Progress Tracker
    ScrollTrigger.create({
      trigger: containerRef.current,
      start: 'top top',
      end: 'bottom bottom',
      onUpdate: (self) => {
        setScrollProgress(self.progress);
      },
    });

    // 2. Hero Text Staggered Reveal
    if (heroTextRef.current) {
      gsap.fromTo(
        heroTextRef.current.children,
        { opacity: 0, y: 40 },
        {
          opacity: 1,
          y: 0,
          duration: 0.8,
          stagger: 0.1,
          ease: 'expo.out',
          clearProps: 'transform',
        }
      );
    }

    // 3. Pinned 3D Scrollytelling Section Choreography (4 Chapters)
    const chapterEls = gsap.utils.toArray('.scrolly-chapter-step');
    chapterEls.forEach((el, index) => {
      ScrollTrigger.create({
        trigger: el,
        start: 'top 55%',
        end: 'bottom 55%',
        onEnter: () => setActiveChapter(index + 1),
        onEnterBack: () => setActiveChapter(index + 1),
      });
    });

    // Reset to Hero (0) when near the top
    ScrollTrigger.create({
      trigger: '#section-hero',
      start: 'top 40%',
      end: 'bottom 40%',
      onEnter: () => setActiveChapter(0),
      onEnterBack: () => setActiveChapter(0),
    });

    // 4. Floating HUD Section Spy (01 - 06)
    const hudSections = [
      { id: 'section-hero', label: '01' },
      { id: 'section-scrollytelling', label: '02' },
      { id: 'section-ecosystem', label: '03' },
      { id: 'section-bento', label: '04' },
      { id: 'section-marquee', label: '05' },
      { id: 'section-cta', label: '06' },
    ];

    hudSections.forEach(({ id, label }) => {
      ScrollTrigger.create({
        trigger: `#${id}`,
        start: 'top 45%',
        end: 'bottom 45%',
        onEnter: () => setActiveHudSection(label),
        onEnterBack: () => setActiveHudSection(label),
      });
    });

    // 5. Stagger reveal for Bento Cards
    const bentoCards = gsap.utils.toArray('.bento-feature-card');
    bentoCards.forEach((card) => {
      gsap.fromTo(
        card,
        { opacity: 0, y: 35 },
        {
          opacity: 1,
          y: 0,
          duration: 0.6,
          ease: 'power3.out',
          scrollTrigger: {
            trigger: card,
            start: 'top 88%',
            toggleActions: 'play none none reverse',
          },
        }
      );
    });

  });

  // Smooth scroll handler for HUD
  const handleScrollToSection = (sectionId) => {
    const el = document.getElementById(sectionId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  // Interactive 3D Cursor-Tilt physics for Role Preview Card
  const handleCardMouseMove = (e) => {
    if (!tiltCardRef.current) return;
    const rect = tiltCardRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const rotX = -((y - centerY) / centerY) * 7.5;
    const rotY = ((x - centerX) / centerX) * 7.5;

    tiltCardRef.current.style.transform = `perspective(1000px) rotateX(${rotX}deg) rotateY(${rotY}deg) scale(1.02)`;
    tiltCardRef.current.style.setProperty('--sheen-x', `${(x / rect.width) * 100}%`);
    tiltCardRef.current.style.setProperty('--sheen-y', `${(y / rect.height) * 100}%`);
  };

  const handleCardMouseLeave = () => {
    if (!tiltCardRef.current) return;
    tiltCardRef.current.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale(1)';
  };

  // Mouse spotlight border tracking for Bento Cards
  const handleBentoCardMouseMove = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    e.currentTarget.style.setProperty('--mouse-x', `${e.clientX - rect.left}px`);
    e.currentTarget.style.setProperty('--mouse-y', `${e.clientY - rect.top}px`);
  };

  // Role Metadata
  const rolesData = {
    buyer: {
      tag: 'NGƯỜI MUA SẮM (BUYER)',
      title: 'Trải Nghiệm Mua Sắm Liền Mạch & Ký Quỹ Bảo Vệ 72H',
      desc: 'Dành cho khách hàng cá nhân và doanh nghiệp tìm kiếm sản phẩm chính hãng với giỏ hàng đa gian hàng thống nhất, thanh toán VietQR tức thì và bảo vệ dòng tiền tuyệt đối.',
      steps: [
        { icon: Search, title: 'Tìm Kiếm AI Ngữ Cảnh', desc: 'Nhập ngôn ngữ tự nhiên, AI tự động so sánh thông số kỹ thuật và đề xuất mã giảm giá cao nhất.' },
        { icon: ShoppingBag, title: '1 Giỏ Hàng Gom Mọi Shop', desc: 'Thêm hàng loạt sản phẩm từ các nhà bán khác nhau và thanh toán duy nhất một lần.' },
        { icon: QrCode, title: 'VietQR NAPAS 3 Giây', desc: 'Quét mã ngân hàng tự động đối soát trong 3s, không cần nhập số tài khoản thủ công.' },
        { icon: ShieldCheck, title: 'Bảo Vệ Escrow 72H', desc: 'Tiền tạm giữ tại ví an toàn, người mua hài lòng sau 72 giờ hệ thống mới tất toán cho người bán.' },
      ],
      metrics: [
        { label: 'Sản phẩm chính hãng', value: '50.000+' },
        { label: 'Tốc độ VietQR', value: '3 Giây' },
        { label: 'Đổi trả miễn phí', value: '72 Giờ' },
      ],
      mockPreview: {
        badge: 'Tài khoản: Khách hàng Kim Cương',
        item: 'Dreame L20 Ultra Flagship Robot',
        status: 'Đã thanh toán VietQR • Đang giao 24H',
        amount: '19.990.000₫',
      }
    },
    seller: {
      tag: 'NHÀ BÁN HÀNG (SELLER)',
      title: 'Tự Động Hóa Vận Hành Gian Hàng & In Vận Đơn 1-Click',
      desc: 'Kênh quản trị bán hàng chuẩn Mall dành cho nhà phân phối và thương hiệu: đồng bộ kho SKU, kết nối trực tiếp bưu cục GHN/GHTK và đối soát doanh thu tự động.',
      steps: [
        { icon: Layers, title: 'Quản Lý Biến Thể SKU', desc: 'Thiết lập đa phiên bản màu sắc, dung lượng, giá buôn và lưu trữ ảnh sản phẩm qua MinIO S3.' },
        { icon: Truck, title: 'Đồng Bộ Vận Đơn GHN', desc: 'Tự động tạo mã vận đơn bưu cục, in phiếu gửi hàng chuẩn nhiệt 1-click ngay khi có đơn.' },
        { icon: Zap, title: 'Tạo Chiến Dịch Flash Sale', desc: 'Cấu hình khung giờ vàng giảm sốc, voucher riêng của Shop kết hợp mã voucher toàn sàn.' },
        { icon: CreditCard, title: 'Rút Doanh Thu Minh Bạch', desc: 'Tiền tự động chuyển vào Ví gian hàng ngay sau khi đơn hoàn tất, rút về ngân hàng trong 1s.' },
      ],
      metrics: [
        { label: 'Đơn xử lý mỗi ngày', value: '10.000+' },
        { label: 'Thời gian in vận đơn', value: '1 Giây' },
        { label: 'Phí duy trì gian hàng', value: '0 Đồng' },
      ],
      mockPreview: {
        badge: 'Shop Mall: Dreame Official Store',
        item: '47 Đơn hàng chờ xuất kho GHN',
        status: 'Tự động in vận đơn bưu cục hoàn tất',
        amount: 'Doanh thu hôm nay: 84.500.000₫',
      }
    },
    admin: {
      tag: 'QUẢN TRỊ VIÊN (ADMIN)',
      title: 'Điều Hành Hệ Sinh Thái Microservices & Giám Sát Rủi Ro',
      desc: 'Bảng chỉ huy thời gian thực kiểm soát 8 dịch vụ độc lập, thẩm định hồ sơ người bán KYC, trọng tài giải quyết khiếu nại Escrow và phân tích tăng trưởng GMV.',
      steps: [
        { icon: Database, title: 'Giám Sát 8 Microservices', desc: 'Theo dõi trực quan trạng thái Gateway, Catalog, Order, Payment, Shipping và AI Service.' },
        { icon: UserCheck, title: 'Kiểm Duyệt Hồ Sơ KYC', desc: 'Thẩm định giấy phép kinh doanh, tem bảo hành chính ngạch trước khi cấp nhãn Mall.' },
        { icon: Shield, title: 'Trọng Tài Giải Quyết Khiếu Nại', desc: 'Can thiệp giữ hoặc hoàn quỹ Escrow ngay tức thì khi phát hiện dấu hiệu gian lận.' },
        { icon: Activity, title: 'Phân Tích Dòng Tiền GMV', desc: 'Báo cáo doanh số toàn sàn, biểu đồ chuyển đổi đơn hàng và dự báo tồn kho thời gian thực.' },
      ],
      metrics: [
        { label: 'Uptime hệ thống', value: '99.99%' },
        { label: 'Cổng Microservices', value: '8 Cổng' },
        { label: 'Độ trễ đối soát', value: '< 100ms' },
      ],
      mockPreview: {
        badge: 'Admin Control Center • Cluster VN-East',
        item: '8/8 Microservices Đang Hoạt Động',
        status: 'Redis Lock: Active • Kafka Bus: Healthy',
        amount: 'Tổng GMV tháng: 12.8 Tỷ VNĐ',
      }
    }
  };

  const currentRoleInfo = rolesData[activeRole];

  return (
    <div 
      ref={containerRef}
      className="min-h-screen bg-[#0A0F1D] text-white font-sans selection:bg-orange-500 selection:text-white relative overflow-hidden"
    >
      {/* Ambient Radial Spotlight Background Glow */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[650px] bg-gradient-to-b from-orange-500/15 via-amber-500/5 to-transparent blur-[140px] pointer-events-none z-0" />
      <div className="absolute top-[35%] right-[-10%] w-[650px] h-[650px] bg-blue-500/10 blur-[150px] pointer-events-none z-0" />
      <div className="absolute top-[70%] left-[-10%] w-[650px] h-[650px] bg-orange-600/10 blur-[160px] pointer-events-none z-0" />

      {/* =========================================================================
          FLOATING CHAPTER HUD INDICATOR (01 - 06)
          ========================================================================= */}
      <nav 
        aria-label="Chapter Indicator"
        className="fixed right-3 sm:right-6 top-1/2 -translate-y-1/2 z-40 hidden lg:flex flex-col items-center gap-3 bg-slate-950/70 p-2.5 rounded-full border border-slate-800/90 backdrop-blur-xl shadow-2xl"
      >
        <div className="text-[9px] font-mono font-bold text-slate-500 uppercase tracking-widest pb-1">
          CH
        </div>

        {[
          { id: 'section-hero', num: '01', title: 'Khởi Nguyên (Hero)' },
          { id: 'section-scrollytelling', num: '02', title: 'Kiến Trúc 3D (Chapters)' },
          { id: 'section-ecosystem', num: '03', title: 'Hệ Sinh Thái 3 Vai Trò' },
          { id: 'section-bento', num: '04', title: 'Ma Trận Bento Kỹ Thuật' },
          { id: 'section-marquee', num: '05', title: 'Chuỗi Cung Ứng & Đối Tác' },
          { id: 'section-cta', num: '06', title: 'Trải Nghiệm Mua Sắm' },
        ].map((item) => {
          const isActive = activeHudSection === item.num;
          return (
            <button
              key={item.id}
              onClick={() => handleScrollToSection(item.id)}
              className="group relative flex items-center justify-center p-1 cursor-pointer transition-all"
              title={item.title}
            >
              <div
                className={`w-7 h-7 rounded-full flex items-center justify-center font-mono text-[10px] font-black transition-all ${
                  isActive
                    ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-lg shadow-orange-500/40 scale-110 ring-2 ring-orange-400/50'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                {item.num}
              </div>

              {/* Tooltip Label on Hover */}
              <span className="absolute right-10 px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-700 text-white text-xs font-semibold whitespace-nowrap opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity shadow-lg">
                {item.title}
              </span>
            </button>
          );
        })}
      </nav>

      {/* =========================================================================
          SECTION 1: HERO STAGE & EDITORIAL HEADLINE
          ========================================================================= */}
      <section 
        id="section-hero"
        className="relative min-h-[92vh] sm:min-h-screen flex items-center z-10 px-4 sm:px-8 max-w-7xl mx-auto"
      >
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center w-full py-12 lg:py-0">
          
          {/* Left Column: Editorial Content (7 cols) */}
          <div ref={heroTextRef} className="lg:col-span-7 space-y-6 z-20">
            {/* Status Pill */}
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-slate-900/90 border border-orange-500/30 backdrop-blur-md shadow-lg shadow-orange-500/10">
              <span className="w-2 h-2 rounded-full bg-orange-500 animate-ping" />
              <span className="text-[11px] font-mono font-bold tracking-wider text-orange-400 uppercase">
                QUICKSHOP 2026 • FLAGSHIP COMMERCE ECOSYSTEM
              </span>
            </div>

            {/* Main Headline */}
            <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-white leading-[1.08] font-sans">
              Định Nghĩa Lại <br />
              Trải Nghiệm{' '}
              <span className="text-orange-500">
                Mua Sắm Trực Tuyến
              </span>
            </h1>

            {/* Editorial Description */}
            <p className="text-sm sm:text-base text-slate-300 leading-relaxed max-w-xl font-normal">
              Nền tảng thương mại điện tử đa gian hàng xây dựng trên kiến trúc microservices hiệu năng cao: điều phối đơn hàng thông minh bưu cục GHN/GHTK, ký quỹ bảo vệ người mua 72h và trợ lý AI cá nhân hóa.
            </p>

            {/* Two-Way Navigation CTA Buttons */}
            <div className="flex flex-wrap items-center gap-4 pt-3">
              {/* Primary CTA: "Mua sắm ngay" */}
              <button
                type="button"
                onClick={onNavigateToHome}
                className="btn-shimmer group relative px-8 py-3.5 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white font-bold text-sm sm:text-base rounded-2xl shadow-xl shadow-orange-500/25 active:scale-95 transition-all flex items-center gap-2.5 cursor-pointer"
              >
                <ShoppingBag className="w-5 h-5 text-white" />
                <span>Mua sắm ngay</span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </button>

              {/* Secondary CTA: "Khám phá hành trình" */}
              <button
                type="button"
                onClick={() => handleScrollToSection('section-scrollytelling')}
                className="px-6 py-3.5 bg-slate-900/80 hover:bg-slate-800 text-slate-200 hover:text-white font-bold text-sm sm:text-base rounded-2xl border border-slate-700/80 backdrop-blur-md active:scale-95 transition-all flex items-center gap-2 cursor-pointer"
              >
                <span>Khám phá hành trình</span>
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

          {/* Right Column: Hero Stage Preview & Cue */}
          <div className="lg:col-span-5 h-[380px] sm:h-[480px] lg:h-[560px] w-full relative flex items-center justify-center">
            <Intro3DErrorBoundary fallback={<Intro2DFallback />}>
              {hasWebGL && activeChapter === 0 ? (
                <Suspense fallback={<Intro2DFallback />}>
                  <SingleIntroCanvas activeChapter={activeChapter} scrollProgress={scrollProgress} />
                </Suspense>
              ) : (
                <Intro2DFallback />
              )}
            </Intro3DErrorBoundary>

            {/* Subtle floating cue */}
            <div className="absolute bottom-3 right-3 bg-slate-950/70 border border-slate-800 text-[10px] font-mono text-slate-400 px-3 py-1 rounded-full backdrop-blur-md pointer-events-none select-none flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-orange-500 animate-pulse" />
              <span>Tương tác 3D đa chiều</span>
            </div>
          </div>

        </div>

        {/* Animated Scroll-Down Cue Line */}
        <button
          type="button"
          onClick={() => handleScrollToSection('section-scrollytelling')}
          className="absolute bottom-6 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2 cursor-pointer select-none text-slate-400 hover:text-orange-400 transition-colors bg-transparent border-0 p-0"
        >
          <span className="text-[10px] font-mono uppercase tracking-widest">Cuộn để khám phá</span>
          <div className="w-5 h-9 rounded-full border border-slate-700 flex justify-center p-1">
            <div className="w-1.5 h-2 bg-orange-500 rounded-full animate-pulse" />
          </div>
        </button>
      </section>

      {/* =========================================================================
          SECTION 2: PINNED 3D SCROLLYTELLING (4 DETAILED CHAPTERS)
          ========================================================================= */}
      <section 
        id="section-scrollytelling" 
        ref={scrollyContainerRef}
        className="relative py-24 px-4 sm:px-8 max-w-7xl mx-auto z-10"
      >
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-3 mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/30 text-orange-400 text-xs font-mono font-bold uppercase tracking-wider">
            <Sparkles className="w-3.5 h-3.5" />
            KIẾN TRÚC & ĐỘT PHÁ
          </div>
          <h2 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight">
            Hành Trình 4 Cột Trụ Đột Phá
          </h2>
          <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
            Mô hình 3D biến đổi liên tục theo từng bước cuộn, trực quan hóa chuỗi công nghệ đột phá tạo nên sức mạnh của QuickShop.
          </p>
        </div>

        {/* Scrollytelling Grid: Left Steps, Right Dynamic Stage */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Left Column: 4 Detailed Chapter Steps */}
          <div className="lg:col-span-7 space-y-24 py-8">
            
            {/* CHAPTER 1 */}
            <div className="scrolly-chapter-step bg-slate-900/60 backdrop-blur-xl border border-slate-800 hover:border-orange-500/50 rounded-3xl p-6 sm:p-9 space-y-5 shadow-2xl transition-all">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-bold text-orange-400 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/20">
                  CHƯƠNG 01 · MULTI-VENDOR
                </span>
                <span className="text-[11px] font-mono text-slate-400">Microservice Catalog</span>
              </div>

              <h3 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
                Hệ Sinh Thái Đa Gian Hàng Thống Nhất
              </h3>

              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                Người mua thỏa sức gom hàng từ nhiều gian hàng chính hãng khác nhau trên toàn quốc và thanh toán chung trong một giỏ hàng duy nhất. Hệ thống tự động phân tách đơn hàng, kiểm soát tồn kho SKU theo thời gian thực và tự động tính toán phí vận chuyển riêng biệt cho từng Shop.
              </p>

              {/* Technical Badges */}
              <div className="flex flex-wrap gap-2 pt-1">
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  PostgreSQL Distributed
                </span>
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  MinIO S3 Storage
                </span>
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  Redis Tier-2 Cache
                </span>
              </div>

              {/* Metrics Grid */}
              <div className="grid grid-cols-3 gap-3 pt-3 border-t border-slate-800">
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-orange-400">50+</div>
                  <div className="text-[10px] text-slate-400">Thương Hiệu Mall</div>
                </div>
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-orange-400">9</div>
                  <div className="text-[10px] text-slate-400">Ngành Hàng Chuẩn</div>
                </div>
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-orange-400">1</div>
                  <div className="text-[10px] text-slate-400">Giỏ Hàng Chung</div>
                </div>
              </div>
            </div>

            {/* CHAPTER 2 */}
            <div className="scrolly-chapter-step bg-slate-900/60 backdrop-blur-xl border border-slate-800 hover:border-blue-500/50 rounded-3xl p-6 sm:p-9 space-y-5 shadow-2xl transition-all">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-bold text-blue-400 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/20">
                  CHƯƠNG 02 · SMART LOGISTICS
                </span>
                <span className="text-[11px] font-mono text-slate-400">Shipping Webhook Router</span>
              </div>

              <h3 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
                Điều Phối Vận Chuyển Từng Kiện Hàng
              </h3>

              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                Tự động tách kiện hàng thông minh theo địa chỉ kho từng nhà bán. Kết nối webhook thời gian thực với GHN Express, GHTK và Viettel Post. Người dùng theo dõi trực tiếp vị trí đơn hàng qua bản đồ GPS và tiến trình 5 giai đoạn giao hàng minh bạch.
              </p>

              {/* Technical Badges */}
              <div className="flex flex-wrap gap-2 pt-1">
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  GHN Express API
                </span>
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  GPS Real-time Tracking
                </span>
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  Auto-split Fulfillment
                </span>
              </div>

              {/* Metrics Grid */}
              <div className="grid grid-cols-3 gap-3 pt-3 border-t border-slate-800">
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-blue-400">2H</div>
                  <div className="text-[10px] text-slate-400">Hỏa Tốc Nội Thành</div>
                </div>
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-blue-400">24H</div>
                  <div className="text-[10px] text-slate-400">Giao Toàn Quốc</div>
                </div>
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-blue-400">100%</div>
                  <div className="text-[10px] text-slate-400">Bảo Hiểm Hàng Hóa</div>
                </div>
              </div>
            </div>

            {/* CHAPTER 3 */}
            <div className="scrolly-chapter-step bg-slate-900/60 backdrop-blur-xl border border-slate-800 hover:border-emerald-500/50 rounded-3xl p-6 sm:p-9 space-y-5 shadow-2xl transition-all">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-bold text-emerald-400 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20">
                  CHƯƠNG 03 · ESCROW TRUST SHIELD
                </span>
                <span className="text-[11px] font-mono text-slate-400">Payment & Escrow Engine</span>
              </div>

              <h3 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
                Thanh Toán VietQR & Bảo Vệ Ký Quỹ 72H
              </h3>

              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                Tạo mã VietQR động chuẩn NAPAS 24/7 với xác nhận tự động trong 3 giây. Toàn bộ tiền thanh toán được đưa vào Ví Ký Quỹ Escrow an toàn. Người bán chỉ nhận tiền khi người mua nhận đúng hàng và không có khiếu nại sau 72 giờ.
              </p>

              {/* Technical Badges */}
              <div className="flex flex-wrap gap-2 pt-1">
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  NAPAS VietQR Pro
                </span>
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  72H Escrow Smart Safe
                </span>
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  Zero-fraud Polling
                </span>
              </div>

              {/* Metrics Grid */}
              <div className="grid grid-cols-3 gap-3 pt-3 border-t border-slate-800">
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-emerald-400">3s</div>
                  <div className="text-[10px] text-slate-400">Xác Nhận QR</div>
                </div>
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-emerald-400">72h</div>
                  <div className="text-[10px] text-slate-400">Ký Quỹ An Toàn</div>
                </div>
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-emerald-400">0%</div>
                  <div className="text-[10px] text-slate-400">Rủi Ro Gian Lận</div>
                </div>
              </div>
            </div>

            {/* CHAPTER 4 */}
            <div className="scrolly-chapter-step bg-slate-900/60 backdrop-blur-xl border border-slate-800 hover:border-amber-500/50 rounded-3xl p-6 sm:p-9 space-y-5 shadow-2xl transition-all">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-bold text-amber-400 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20">
                  CHƯƠNG 04 · AI CONCIERGE
                </span>
                <span className="text-[11px] font-mono text-slate-400">Notification & AI Service</span>
              </div>

              <h3 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
                Trợ Lý Mua Sắm AI Tích Hợp
              </h3>

              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                Trí tuệ nhân tạo QuickShop AI phân tích sở thích, tìm kiếm thông minh bằng ngôn ngữ tự nhiên, so sánh giá đa gian hàng tức thì và tự động phát hiện mã giảm giá tối ưu nhất cho từng đơn hàng của bạn.
              </p>

              {/* Technical Badges */}
              <div className="flex flex-wrap gap-2 pt-1">
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  LLM Reasoning Core
                </span>
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  Vector Semantic Search
                </span>
                <span className="px-2.5 py-1 rounded-lg bg-slate-800 text-[11px] font-mono font-semibold text-slate-300 border border-slate-700">
                  Real-time Deal Matcher
                </span>
              </div>

              {/* Metrics Grid */}
              <div className="grid grid-cols-3 gap-3 pt-3 border-t border-slate-800">
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-amber-400">0.4s</div>
                  <div className="text-[10px] text-slate-400">Tốc Độ Phản Hồi</div>
                </div>
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-amber-400">99.2%</div>
                  <div className="text-[10px] text-slate-400">Độ Chuẩn Tìm Kiếm</div>
                </div>
                <div className="text-center p-2 rounded-xl bg-slate-950/50">
                  <div className="text-base sm:text-lg font-black font-mono text-amber-400">-35%</div>
                  <div className="text-[10px] text-slate-400">Tiết Kiệm Chi Phí</div>
                </div>
              </div>
            </div>

          </div>

          {/* Right Column: Sticky 3D Showcase Stage */}
          <div className="lg:col-span-5 sticky top-28 h-[480px] sm:h-[580px] w-full hidden lg:flex items-center justify-center rounded-3xl bg-slate-950/40 border border-slate-800/80 p-2 shadow-2xl backdrop-blur-md">
            <Intro3DErrorBoundary fallback={<Intro2DFallback />}>
              {hasWebGL && activeChapter > 0 ? (
                <Suspense fallback={<Intro2DFallback />}>
                  <SingleIntroCanvas activeChapter={activeChapter} scrollProgress={scrollProgress} />
                </Suspense>
              ) : (
                <Intro2DFallback />
              )}
            </Intro3DErrorBoundary>

            {/* Chapter Badge Overlay */}
            <div className="absolute top-4 left-4 bg-slate-900/80 border border-slate-700/80 px-3 py-1.5 rounded-full text-xs font-mono font-bold flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-orange-500 animate-pulse" />
              <span className="text-white">
                CHƯƠNG {activeChapter === 0 ? '01' : `0${activeChapter}`} / 04
              </span>
            </div>
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 3: INTERACTIVE 3-ROLE ECOSYSTEM EXPLORER
          ========================================================================= */}
      <section 
        id="section-ecosystem"
        className="py-24 px-4 sm:px-8 max-w-7xl mx-auto relative z-10"
      >
        <div className="text-center max-w-2xl mx-auto space-y-3 mb-12">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/30 text-orange-400 text-xs font-mono font-bold uppercase tracking-wider">
            <Layers className="w-3.5 h-3.5" />
            ĐỒNG BỘ 3 PHÂN HỆ
          </div>
          <h2 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight">
            Một Nền Tảng Cho Cả Ba Vai Trò
          </h2>
          <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
            Khám phá quy trình chuyên biệt được thiết kế may đo cho từng người tham gia hệ sinh thái QuickShop.
          </p>
        </div>

        {/* Role Switcher Pill Bar */}
        <div className="flex justify-center mb-12">
          <div className="inline-flex items-center p-1.5 bg-slate-900/90 rounded-2xl border border-slate-800 shadow-xl">
            {[
              { key: 'buyer', label: 'Người Mua Sắm', icon: ShoppingBag },
              { key: 'seller', label: 'Nhà Bán Hàng (Seller)', icon: Store },
              { key: 'admin', label: 'Quản Trị Viên (Admin)', icon: ShieldCheck },
            ].map(({ key, label, icon: Icon }) => {
              const isSelected = activeRole === key;
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => setActiveRole(key)}
                  className={`px-4 sm:px-6 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2 cursor-pointer ${
                    isSelected
                      ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-lg shadow-orange-500/30 scale-102'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Role Content Showcase: 2 Columns */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center bg-slate-900/50 border border-slate-800 rounded-3xl p-6 sm:p-12 backdrop-blur-xl">
          
          {/* Left Column: Workflow Steps & Details */}
          <div className="lg:col-span-7 space-y-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/30 text-orange-400 text-xs font-mono font-bold">
              {currentRoleInfo.tag}
            </div>

            <h3 className="text-2xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight">
              {currentRoleInfo.title}
            </h3>

            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
              {currentRoleInfo.desc}
            </p>

            {/* 4 Steps */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
              {currentRoleInfo.steps.map((st, i) => {
                const StepIcon = st.icon;
                return (
                  <div key={i} className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800 space-y-2">
                    <div className="flex items-center gap-2.5 text-orange-400">
                      <div className="w-7 h-7 rounded-lg bg-orange-500/15 flex items-center justify-center">
                        <StepIcon className="w-4 h-4" />
                      </div>
                      <span className="text-xs font-bold text-white">{st.title}</span>
                    </div>
                    <p className="text-[11px] text-slate-400 leading-relaxed">{st.desc}</p>
                  </div>
                );
              })}
            </div>

            {/* Metrics */}
            <div className="pt-4 flex flex-wrap items-center gap-6 border-t border-slate-800">
              {currentRoleInfo.metrics.map((m, i) => (
                <div key={i}>
                  <div className="text-lg sm:text-xl font-black font-mono text-orange-400">{m.value}</div>
                  <div className="text-[11px] text-slate-400">{m.label}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Right Column: Interactive 3D Cursor-Tilt Preview Card */}
          <div className="lg:col-span-5 flex justify-center">
            <div
              ref={tiltCardRef}
              onMouseMove={handleCardMouseMove}
              onMouseLeave={handleCardMouseLeave}
              className="w-full max-w-sm rounded-3xl bg-gradient-to-b from-slate-900 via-slate-950 to-slate-900 border border-orange-500/30 p-6 shadow-2xl relative select-none transition-transform duration-150 ease-out cursor-pointer overflow-hidden"
              style={{
                transformStyle: 'preserve-3d',
              }}
            >
              {/* Dynamic Specular Sheen Glow */}
              <div 
                className="absolute inset-0 pointer-events-none opacity-40 transition-opacity"
                style={{
                  background: 'radial-gradient(400px circle at var(--sheen-x, 50%) var(--sheen-y, 50%), rgba(249,115,22,0.25), transparent 80%)'
                }}
              />

              <div className="relative z-10 space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                  <div className="text-xs font-mono font-bold text-orange-400 flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                    <span>{currentRoleInfo.mockPreview.badge}</span>
                  </div>
                  <Zap className="w-4 h-4 text-orange-500" />
                </div>

                <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800/80 space-y-2">
                  <div className="text-[11px] font-mono text-slate-400">TIÊU ĐIỂM HOẠT ĐỘNG</div>
                  <div className="text-sm font-bold text-white">{currentRoleInfo.mockPreview.item}</div>
                  <div className="text-xs text-emerald-400 font-medium">{currentRoleInfo.mockPreview.status}</div>
                </div>

                <div className="p-4 rounded-2xl bg-orange-500/10 border border-orange-500/20 text-center">
                  <div className="text-xs text-orange-400 font-mono">DỮ LIỆU THỜI GIAN THỰC</div>
                  <div className="text-xl font-mono font-black text-white mt-1">
                    {currentRoleInfo.mockPreview.amount}
                  </div>
                </div>

                <div className="text-center text-[10px] text-slate-500 font-mono">
                  Di chuột để kiểm tra hiệu ứng nghiêng 3D
                </div>
              </div>
            </div>
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 4: DEEP-DIVE BENTO FEATURE MATRIX
          ========================================================================= */}
      <section 
        id="section-bento"
        className="py-24 px-4 sm:px-8 max-w-7xl mx-auto relative z-10"
      >
        <div className="text-center max-w-2xl mx-auto space-y-3 mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/30 text-orange-400 text-xs font-mono font-bold uppercase tracking-wider">
            <Cpu className="w-3.5 h-3.5" />
            CÔNG NGHỆ CỐT LÕI
          </div>
          <h2 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight">
            Ma Trận Tính Năng QuickShop 2026
          </h2>
          <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
            Hội tụ đầy đủ tiêu chuẩn khắt khe nhất của một nền tảng thương mại điện tử thế hệ mới.
          </p>
        </div>

        {/* 6 Asymmetrical Bento Cards with Mouse-Tracking Spotlight Borders */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          
          {/* Card 1: Multi-Tier Voucher Engine (Col Span 2) */}
          <div 
            onMouseMove={handleBentoCardMouseMove}
            className="bento-feature-card lg:col-span-2 relative p-7 sm:p-9 rounded-3xl bg-slate-900/60 border border-slate-800 hover:border-orange-500/50 backdrop-blur-xl transition-all duration-300 shadow-xl overflow-hidden group"
          >
            <div className="flex items-center justify-between pb-3">
              <div className="w-10 h-10 rounded-xl bg-orange-500/15 flex items-center justify-center text-orange-400">
                <CreditCard className="w-5 h-5" />
              </div>
              <span className="text-xs font-mono font-bold text-orange-400 bg-orange-500/10 px-2.5 py-1 rounded-lg">
                MULTI-TIER VOUCHER
              </span>
            </div>
            <h3 className="text-xl sm:text-2xl font-bold text-white mt-2">
              Hệ Thống Voucher Đa Tầng Xếp Chồng
            </h3>
            <p className="text-xs sm:text-sm text-slate-300 mt-2 leading-relaxed max-w-xl">
              Áp dụng đồng thời 3 tầng ưu đãi trong một đơn hàng: Voucher toàn sàn QuickShop + Voucher độc quyền của Shop + Mã miễn phí vận chuyển. Tự động tính toán phương án giảm giá sâu nhất cho khách hàng.
            </p>
            {/* Visual Voucher Stack */}
            <div className="mt-5 grid grid-cols-3 gap-3">
              <div className="p-3 rounded-xl bg-slate-950/70 border border-orange-500/30 text-center">
                <div className="text-xs font-mono font-bold text-orange-400">-35%</div>
                <div className="text-[10px] text-slate-400">Mã Sàn QuickShop</div>
              </div>
              <div className="p-3 rounded-xl bg-slate-950/70 border border-amber-500/30 text-center">
                <div className="text-xs font-mono font-bold text-amber-400">-50.000₫</div>
                <div className="text-[10px] text-slate-400">Voucher Gian Hàng</div>
              </div>
              <div className="p-3 rounded-xl bg-slate-950/70 border border-emerald-500/30 text-center">
                <div className="text-xs font-mono font-bold text-emerald-400">FREE SHIP</div>
                <div className="text-[10px] text-slate-400">Toàn Quốc 0đ</div>
              </div>
            </div>
          </div>

          {/* Card 2: Guest-to-User State Migration */}
          <div 
            onMouseMove={handleBentoCardMouseMove}
            className="bento-feature-card relative p-7 rounded-3xl bg-slate-900/60 border border-slate-800 hover:border-blue-500/50 backdrop-blur-xl transition-all duration-300 shadow-xl overflow-hidden group"
          >
            <div className="w-10 h-10 rounded-xl bg-blue-500/15 flex items-center justify-center text-blue-400 mb-4">
              <RefreshCw className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-bold text-white">Đồng Bộ Giỏ Hàng Xuyên Thiết Bị</h3>
            <p className="text-xs text-slate-300 mt-2 leading-relaxed">
              Khách vãng lai gom hàng tự do. Khi đăng nhập, toàn bộ giỏ hàng cục bộ tự động hợp nhất với tài khoản trên cơ sở dữ liệu đám mây mà không thất lạc món nào.
            </p>
            <div className="mt-4 p-2.5 rounded-xl bg-slate-950/70 border border-slate-800 text-[11px] font-mono text-blue-400 flex items-center justify-between">
              <span>Guest Cart ➔ User Sync</span>
              <span className="text-emerald-400">Hoàn tất</span>
            </div>
          </div>

          {/* Card 3: Global Keyboard Shortcuts */}
          <div 
            onMouseMove={handleBentoCardMouseMove}
            className="bento-feature-card relative p-7 rounded-3xl bg-slate-900/60 border border-slate-800 hover:border-amber-500/50 backdrop-blur-xl transition-all duration-300 shadow-xl overflow-hidden group"
          >
            <div className="w-10 h-10 rounded-xl bg-amber-500/15 flex items-center justify-center text-amber-400 mb-4">
              <Search className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-bold text-white">Phím Tắt Toàn Cục `/` & `Ctrl+K`</h3>
            <p className="text-xs text-slate-300 mt-2 leading-relaxed">
              Tra cứu nhanh như coder. Nhấn `/` hoặc `Ctrl+K` ở bất kỳ đâu để kích hoạt thanh tìm kiếm spotlight, gõ tên sản phẩm và duyệt kết quả tức thì.
            </p>
            <div className="mt-4 flex gap-2">
              <span className="px-3 py-1 rounded-lg bg-slate-950 border border-slate-700 font-mono text-xs font-bold text-white">/</span>
              <span className="px-3 py-1 rounded-lg bg-slate-950 border border-slate-700 font-mono text-xs font-bold text-white">Ctrl + K</span>
            </div>
          </div>

          {/* Card 4: Distributed Idempotency Key */}
          <div 
            onMouseMove={handleBentoCardMouseMove}
            className="bento-feature-card relative p-7 rounded-3xl bg-slate-900/60 border border-slate-800 hover:border-emerald-500/50 backdrop-blur-xl transition-all duration-300 shadow-xl overflow-hidden group"
          >
            <div className="w-10 h-10 rounded-xl bg-emerald-500/15 flex items-center justify-center text-emerald-400 mb-4">
              <Lock className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-bold text-white">Chống Trùng Lặp Đơn Hàng</h3>
            <p className="text-xs text-slate-300 mt-2 leading-relaxed">
              Khóa phân tán Redis Lock kết hợp Idempotency Key ngăn ngừa hoàn toàn tình trạng bấm đúp trừ tiền hai lần hoặc sinh đơn hàng trùng lặp khi mạng chập chờn.
            </p>
            <div className="mt-4 text-[11px] font-mono text-emerald-400 flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Redis Lock 3000ms an toàn</span>
            </div>
          </div>

          {/* Card 5: Real-time Logistics Stepper */}
          <div 
            onMouseMove={handleBentoCardMouseMove}
            className="bento-feature-card relative p-7 rounded-3xl bg-slate-900/60 border border-slate-800 hover:border-orange-500/50 backdrop-blur-xl transition-all duration-300 shadow-xl overflow-hidden group"
          >
            <div className="w-10 h-10 rounded-xl bg-orange-500/15 flex items-center justify-center text-orange-400 mb-4">
              <Truck className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-bold text-white">Theo Dõi Vận Chuyển 5 Mốc</h3>
            <p className="text-xs text-slate-300 mt-2 leading-relaxed">
              Cập nhật trực tiếp từ bưu cục GHN: Đã tiếp nhận ➔ Xuất kho ➔ Trung chuyển ➔ Đang giao ➔ Giao thành công kèm mã vận đơn đối soát.
            </p>
            {/* 5-step progress bar */}
            <div className="mt-4 flex items-center gap-1">
              {[1, 2, 3, 4, 5].map((s) => (
                <div key={s} className="h-1.5 flex-1 rounded-full bg-orange-500" />
              ))}
            </div>
          </div>

          {/* Card 6: 9 Merchandise Categories (Col Span 2) */}
          <div 
            onMouseMove={handleBentoCardMouseMove}
            className="bento-feature-card lg:col-span-2 relative p-7 sm:p-9 rounded-3xl bg-slate-900/60 border border-slate-800 hover:border-orange-500/50 backdrop-blur-xl transition-all duration-300 shadow-xl overflow-hidden group"
          >
            <div className="flex items-center justify-between pb-3">
              <div className="w-10 h-10 rounded-xl bg-orange-500/15 flex items-center justify-center text-orange-400">
                <Store className="w-5 h-5" />
              </div>
              <span className="text-xs font-mono font-bold text-orange-400 bg-orange-500/10 px-2.5 py-1 rounded-lg">
                CURATED MALL CATALOG
              </span>
            </div>
            <h3 className="text-xl sm:text-2xl font-bold text-white mt-2">
              Hệ Sinh Thái 9 Ngành Hàng Flagship
            </h3>
            <p className="text-xs sm:text-sm text-slate-300 mt-2 leading-relaxed max-w-xl">
              Danh mục tuyển chọn chính hãng từ các thương hiệu công nghệ, thời trang, thiết bị gia dụng và phong cách sống hàng đầu với cam kết xuất xứ rõ ràng.
            </p>
            <div className="mt-5 grid grid-cols-3 sm:grid-cols-5 gap-2.5 text-xs font-medium text-slate-300">
              {['Điện Thoại', 'Laptop M3', 'Thời Trang', 'Mỹ Phẩm', 'Gia Dụng', 'Sách Vở', 'Thể Thao', 'Giày Dép', 'Đồ Điện Tử'].map((cat, idx) => (
                <div key={idx} className="p-2 rounded-xl bg-slate-950/70 border border-slate-800 text-center hover:border-orange-500/40 transition-colors">
                  {cat}
                </div>
              ))}
            </div>
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 5: SCROLL-DRIVEN HORIZONTAL MARQUEE
          ========================================================================= */}
      <section 
        id="section-marquee"
        className="py-20 border-y border-slate-800/80 bg-slate-950/70 relative z-10 overflow-hidden"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-8 mb-6 flex items-center justify-between">
          <div className="text-xs font-mono font-bold uppercase tracking-wider text-orange-400 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>MẠNG LƯỚI ĐỐI TÁC CHIẾN LƯỢC & THƯƠNG HIỆU CHÍNH HÃNG</span>
          </div>
          <span className="text-xs text-slate-500 font-mono hidden sm:inline">100% Mall Verified</span>
        </div>

        {/* Continuous Fluid Horizontal Marquee Track */}
        <div className="relative w-full overflow-hidden py-3">
          <div className="flex gap-4 w-max animate-marquee">
            {[
              'QuickShop Mall 2026',
              'Apple Authorised',
              'Dreame Official',
              'Samsung Flagship',
              'GHN Express Official',
              'Giao Hàng Tiết Kiệm GHTK',
              'VietQR NAPAS 24/7',
              'Viettel Post',
              'Nike Official Store',
              'Anker Innovations',
              'Coolmate Cafe Unisex',
              'Ví Escrow Ký Quỹ 72H',
              'QuickShop AI Concierge',
              'MinIO S3 Cloud Storage',
              'Spring Boot 3.3 Microservices',
            ].concat([
              'QuickShop Mall 2026',
              'Apple Authorised',
              'Dreame Official',
              'Samsung Flagship',
              'GHN Express Official',
              'Giao Hàng Tiết Kiệm GHTK',
              'VietQR NAPAS 24/7',
              'Viettel Post',
              'Nike Official Store',
              'Anker Innovations',
              'Coolmate Cafe Unisex',
              'Ví Escrow Ký Quỹ 72H',
              'QuickShop AI Concierge',
              'MinIO S3 Cloud Storage',
              'Spring Boot 3.3 Microservices',
            ]).map((badge, idx) => (
              <div
                key={idx}
                className="px-5 py-2.5 rounded-2xl bg-slate-900 border border-slate-800 text-xs sm:text-sm font-semibold text-slate-300 hover:text-white hover:border-orange-500/40 transition-colors whitespace-nowrap flex items-center gap-2 shadow-sm"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-orange-500" />
                <span>{badge}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 6: GRAND FINALE CTA STAGE
          ========================================================================= */}
      <section 
        id="section-cta"
        className="py-24 px-4 sm:px-8 max-w-7xl mx-auto relative z-10"
      >
        <div className="relative rounded-3xl bg-gradient-to-br from-slate-900 via-slate-950 to-[#0A0F1D] border border-orange-500/40 p-8 sm:p-16 overflow-hidden shadow-2xl shadow-orange-500/10">
          
          {/* Subtle Ambient Radial Glow */}
          <div className="absolute right-0 top-0 translate-x-1/3 -translate-y-1/3 w-[450px] h-[450px] bg-orange-500/20 rounded-full blur-3xl pointer-events-none" />
          
          <div className="relative z-10 max-w-3xl space-y-6">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-orange-500/20 border border-orange-500/30 text-orange-400 text-xs font-mono font-bold uppercase">
              <Zap className="w-3.5 h-3.5" />
              SẴN SÀNG TRẢI NGHIỆM FLAGSHIP COMMERCE
            </div>

            <h2 className="text-3xl sm:text-5xl lg:text-6xl font-black text-white tracking-tight leading-tight">
              Bước Vào Kỷ Nguyên Mua Sắm Cùng QuickShop
            </h2>

            <p className="text-xs sm:text-base text-slate-300 leading-relaxed max-w-2xl">
              Hàng ngàn sản phẩm chính hãng, chương trình Flash Sale sốc mỗi ngày cùng chính sách bảo vệ người mua an toàn 72 giờ đang chờ bạn khám phá.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-4">
              <button
                type="button"
                onClick={onNavigateToHome}
                className="btn-shimmer px-8 py-4 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white font-bold text-sm sm:text-base rounded-2xl shadow-xl shadow-orange-500/30 active:scale-95 transition-all flex items-center gap-2.5 cursor-pointer"
              >
                <ShoppingBag className="w-5 h-5" />
                <span>Vào sàn Mua sắm</span>
                <ArrowRight className="w-4 h-4" />
              </button>

              {onNavigateToRegisterSeller && (
                <button
                  type="button"
                  onClick={onNavigateToRegisterSeller}
                  className="px-6 py-4 bg-slate-900/90 hover:bg-slate-800 text-slate-200 hover:text-white font-bold text-sm sm:text-base rounded-2xl border border-slate-700 backdrop-blur-md active:scale-95 transition-all flex items-center gap-2 cursor-pointer"
                >
                  <Store className="w-4 h-4 text-emerald-400" />
                  <span>Mở Gian Hàng Bán</span>
                </button>
              )}

              {onNavigateToRegisterShipper && (
                <button
                  type="button"
                  onClick={onNavigateToRegisterShipper}
                  className="px-6 py-4 bg-slate-900/90 hover:bg-slate-800 text-slate-200 hover:text-white font-bold text-sm sm:text-base rounded-2xl border border-slate-700 backdrop-blur-md active:scale-95 transition-all flex items-center gap-2 cursor-pointer"
                >
                  <Truck className="w-4 h-4 text-blue-400" />
                  <span>Đăng ký Shipper</span>
                </button>
              )}
            </div>

            {/* Quick Links */}
            <div className="pt-6 border-t border-slate-800/80 flex flex-wrap gap-6 text-xs text-slate-400 font-medium">
              <button onClick={onNavigateToHome} className="hover:text-orange-400 transition-colors">
                Trang Chủ Sàn
              </button>
              <button onClick={onNavigateToLogin} className="hover:text-orange-400 transition-colors">
                Đăng Nhập Tài Khoản
              </button>
              <span>Chính Sách Escrow 72H</span>
              <span>Bảo Hành Điện Tử</span>
            </div>
          </div>
        </div>
      </section>

      {/* Editorial Footer */}
      <footer className="border-t border-slate-800/80 py-10 px-4 sm:px-8 text-center text-xs text-slate-500 relative z-10 space-y-2">
        <p className="font-medium text-slate-400">© 2026 QuickShop Platform. Nền tảng thương mại điện tử Microservices kiến trúc cao cấp.</p>
        <p className="text-[11px] text-slate-600">Được tối ưu hóa với React 18, Three.js, React Three Fiber, GSAP & Tailwind CSS.</p>
      </footer>
    </div>
  );
}
