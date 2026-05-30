/**
 * GSAP 全局注册（遵循 greensock/gsap-skills：插件只注册一次）
 */
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { ScrollToPlugin } from 'gsap/ScrollToPlugin';

gsap.registerPlugin(ScrollTrigger, ScrollToPlugin);

export { gsap, ScrollTrigger, ScrollToPlugin };
