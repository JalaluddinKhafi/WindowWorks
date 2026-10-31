import * as THREE from './vendor/three.module.min.js';
import { OrbitControls } from './vendor/OrbitControls.js';

try {
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
  renderer.setClearColor(0x0f172a);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.25;
  document.body.appendChild(renderer.domElement);
  const scene = new THREE.Scene();
  scene.add(new THREE.HemisphereLight(0xe0f2fe, 0x475569, 2.4));
  const key = new THREE.DirectionalLight(0xffffff, 3.5);
  key.position.set(3, 5, 6); scene.add(key);
  const rim = new THREE.DirectionalLight(0x7dd3fc, 2);
  rim.position.set(-4, 2, -3); scene.add(rim);
  const model = new THREE.Group(); scene.add(model);
  const grid = new THREE.GridHelper(8, 32, 0x475569, 0x1e293b); scene.add(grid);
  let camera, controls, viewKey = '', extent = 2;
  let active = true, frame = 0;
  function draw() { renderer.render(scene, camera); }
  function resize() {
    renderer.setSize(innerWidth, innerHeight);
    if (!camera) return;
    const aspect = innerWidth / Math.max(innerHeight, 1);
    if (camera.isPerspectiveCamera) camera.aspect = aspect;
    else { camera.left = -extent * aspect; camera.right = extent * aspect; camera.top = extent; camera.bottom = -extent; }
    camera.updateProjectionMatrix(); draw();
  }
  window.applyWindow = data => {
    // Rebuild only geometry; orbit position survives edits to a sash or handle.
    while (model.children.length) {
      const child = model.children[0]; model.remove(child);
      child.geometry.dispose(); child.material.dispose();
    }
    extent = Math.max(data.extent / 1000, 0.4);
    for (const face of data.faces) {
      const vertices = [];
      for (const i of [0, 1, 2, 0, 2, 3]) vertices.push(...face.vertices[i].map(v => v / 1000));
      const geometry = new THREE.BufferGeometry();
      geometry.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3));
      geometry.computeVertexNormals();
      const color = new THREE.Color().setRGB(...face.color, THREE.SRGBColorSpace);
      const material = new THREE.MeshStandardMaterial({ color, side: THREE.DoubleSide,
        metalness: face.glass ? 0.15 : 0.25, roughness: face.glass ? 0.08 : 0.38,
        transparent: face.glass, opacity: face.glass ? 0.32 : 1, depthWrite: !face.glass });
      model.add(new THREE.Mesh(geometry, material));
    }
    const bounds = new THREE.Box3().setFromObject(model);
    grid.position.y = bounds.min.y - 0.03;
    const nextKey = [data.yaw, data.pitch, data.zoom, data.perspective, extent, data.resetVersion].join('|');
    if (nextKey !== viewKey) {
      viewKey = nextKey;
      controls?.dispose();
      camera = data.perspective ? new THREE.PerspectiveCamera(40, 1, 0.01, extent * 30)
        : new THREE.OrthographicCamera(-extent, extent, extent, -extent, 0.01, extent * 30);
      const yaw = THREE.MathUtils.degToRad(data.yaw), pitch = THREE.MathUtils.degToRad(data.pitch);
      const distance = extent * 2.2 / data.zoom;
      camera.position.set(Math.sin(yaw) * Math.cos(pitch) * distance, Math.sin(pitch) * distance, Math.cos(yaw) * Math.cos(pitch) * distance);
      if (!data.perspective) camera.zoom = data.zoom;
      controls = new OrbitControls(camera, renderer.domElement);
      controls.enableDamping = true; controls.dampingFactor = 0.1;
      controls.minDistance = extent * 0.3; controls.maxDistance = extent * 8;
      controls.addEventListener('change', draw);
    }
    resize(); controls.update(); draw();
  };
  function animate() {
    if (!active) return;
    controls?.update(); frame = requestAnimationFrame(animate);
  }
  document.addEventListener('visibilitychange', () => {
    active = !document.hidden; cancelAnimationFrame(frame); if (active) animate();
  });
  renderer.domElement.addEventListener('webglcontextlost', event => {
    event.preventDefault(); console.error('WW_RENDERER_UNAVAILABLE');
  });
  addEventListener('resize', resize);
  if (window.pendingWindow) window.applyWindow(window.pendingWindow);
  animate();
} catch (error) { console.error('WW_RENDERER_UNAVAILABLE', error); }
