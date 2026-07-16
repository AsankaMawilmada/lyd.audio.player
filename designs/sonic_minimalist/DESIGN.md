---
name: Sonic Minimalist
colors:
  surface: '#111318'
  surface-dim: '#111318'
  surface-bright: '#37393e'
  surface-container-lowest: '#0c0e13'
  surface-container-low: '#191c20'
  surface-container: '#1e2025'
  surface-container-high: '#282a2f'
  surface-container-highest: '#33353a'
  on-surface: '#e2e2e9'
  on-surface-variant: '#c6c6cb'
  inverse-surface: '#e2e2e9'
  inverse-on-surface: '#2e3036'
  outline: '#909095'
  outline-variant: '#45474b'
  surface-tint: '#c6c6cc'
  primary: '#c6c6cc'
  on-primary: '#2f3035'
  primary-container: '#0f1115'
  on-primary-container: '#7b7c82'
  inverse-primary: '#5d5e63'
  secondary: '#ffb59a'
  on-secondary: '#5a1b00'
  secondary-container: '#ff5e07'
  on-secondary-container: '#531900'
  tertiary: '#c6c6c7'
  on-tertiary: '#2f3131'
  tertiary-container: '#0f1112'
  on-tertiary-container: '#7b7d7d'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e2e2e8'
  primary-fixed-dim: '#c6c6cc'
  on-primary-fixed: '#1a1c20'
  on-primary-fixed-variant: '#45474b'
  secondary-fixed: '#ffdbce'
  secondary-fixed-dim: '#ffb59a'
  on-secondary-fixed: '#370e00'
  on-secondary-fixed-variant: '#802a00'
  tertiary-fixed: '#e2e2e2'
  tertiary-fixed-dim: '#c6c6c7'
  on-tertiary-fixed: '#1a1c1c'
  on-tertiary-fixed-variant: '#454747'
  background: '#111318'
  on-background: '#e2e2e9'
  surface-variant: '#33353a'
typography:
  display:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Manrope
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Manrope
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  display-mobile:
    fontFamily: Manrope
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 36px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
  safe-area: 20px
---

## Brand & Style
The design system is centered on a "Quiet Premium" philosophy. It targets audiophiles and enthusiasts who value a clutter-free environment that prioritizes the emotional connection to music. The aesthetic is a hybrid of **Minimalism** and **Glassmorphism**, emphasizing high-fidelity playback through a reduced visual noise interface.

The emotional response should be one of calm, focus, and modern sophistication. By utilizing generous whitespace and a "content-first" hierarchy, the user’s attention is directed toward the album artistry and the tactile rhythm of the playback controls.

## Colors
The palette utilizes a "Deep Space" foundation to make album art and the primary accent pop.
- **Primary (Deep Charcoal):** Used for the global background and core surfaces to provide depth.
- **Secondary (Sunset Orange):** A vibrant accent used sparingly for active states, playback progress, and critical calls to action.
- **Tertiary (Soft White):** Reserved for primary text and high-contrast iconography.
- **Neutral:** A slightly lighter charcoal used for secondary containers, card backgrounds, and input fields to create subtle layering.

## Typography
This design system employs **Manrope** for its modern, balanced, and highly legible sans-serif qualities. It provides a technical yet friendly feel suitable for a premium utility. 

**JetBrains Mono** is used selectively for technical metadata—such as timestamps, bitrates, and track numbers—to evoke a precision-engineered "pro audio" aesthetic.

## Layout & Spacing
The layout follows a **Fluid Grid** model optimized for mobile-first interaction. 
- **Safe Margins:** A consistent 20px margin is maintained on the left and right edges of the screen.
- **Vertical Rhythm:** A base 4px unit dictates all spacing. Large 48px (2xl) gaps are used to separate major functional groups, such as the Album Art from the Playback Controls, to ensure the UI feels uncrowded.
- **Touch Targets:** All interactive elements (play, skip, shuffle) must maintain a minimum 44x44px hit area regardless of their visual size.

## Elevation & Depth
Depth is achieved through **Tonal Layers** and **Ambient Shadows**. 
- **Surface Level 0:** The deep charcoal background (#0F1115).
- **Surface Level 1:** Raised containers (#1E2025) with a very soft, diffused shadow (15% opacity black, 20px blur, 4px offset).
- **Glassmorphism:** The "Now Playing" bar and overlay modals use a backdrop blur (20px) with a 40% opaque neutral fill to maintain context of the underlying list.
- **Outlines:** Subtle 1px inner borders (stroke) at 10% white opacity are used on cards to define edges without adding heavy visual weight.

## Shapes
The design system uses a high roundedness factor to communicate approachability and premium comfort. 
- **Large Elements:** Album art containers and primary cards use `rounded-2xl` or `rounded-3xl` (1.5rem to 3rem).
- **Small Elements:** Buttons and tags use a full **Pill-shape** to contrast against the rectangular nature of album covers.
- **Consistency:** Interaction states (hover/active) should maintain the same corner radius as the base element.

## Components
- **Playback Controls:** The 'Play/Pause' button is the hero element, utilizing a large pill-shaped container with the Secondary accent color. Skip and Shuffle icons are tertiary white with no background.
- **Scrubbing Bar:** A thin 4px track with a secondary-colored progress fill. The "thumb" should be a 12px soft white circle that appears only during active interaction.
- **Cards (Album/Playlist):** Feature high-radius corners (24px) and a subtle Level 1 elevation. No borders; use tonal difference to separate.
- **Lists:** Clean rows with 16px vertical padding. Use `label-sm` for track duration and `body-md` for artist names in a muted 60% white opacity.
- **Chips:** Small pill-shaped badges for genres or "High-Res" labels, using the Neutral color with `label-sm` typography.
- **Visualizer:** A minimal, 3-bar animated icon in the Secondary color used to indicate the currently playing track in a list view.