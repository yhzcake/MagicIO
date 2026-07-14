---
title: "Zhen Bus and Six-Sided Processors"
navigation:
  title: "Chapter 4"
---

# Chapter 4: Zhen Bus and Six-Sided Processors

## 1. What Is the Zhen Bus?

The Zhen Bus combines up to six surface processors at the position of one non-colliding block. Each surface has its own Zhen type, storage, processing state, and external capability access. Rendering and surface selection use a surface sheet 1/16 of a block thick.

The Zhen Bus itself has no collision, and an empty surface does not provide a processor. When the player aims at it, the code performs ray detection against installed surfaces to reduce the chance of edge clicks selecting the wrong direction.

## 2. Default State After Placement

When the Zhen Bus is placed, the server automatically installs an Unstable Sieve Zhen on the `DOWN` surface. It is not an empty six-sided container. Therefore, when planning a bus for the first time, treat the bottom surface as already occupied.

After the Zhen Bus is broken, the block itself drops according to normal rules, while the bus additionally collects and spawns the items in its six surface processors. Do not use world reload behavior as a substitute for a normal breaking test.

## 3. Installing Processors

Right-click the Zhen Bus while holding any dynamically registered Zhen block. The program reads the Zhen type corresponding to that block ID and installs it on the surface that was hit. Block capabilities are refreshed afterward.

The target surface is normally determined by the ray hit. When the example item with `side` custom data is held in the offhand, it can override the target surface. Right-clicking air with the example item cycles through up, down, north, south, west, and east and sends an English debug message.

**Limitation:** The example item is a development tool. Its related branch also contains a lookup for a Sieve type without the `_zhen` suffix, which does not match the formally registered ID. Therefore, "right-clicking the bus directly with the example item to install a Sieve Zhen" should not be treated as a reliable player workflow. Holding an actual Zhen block in the main hand is more reliable.

## 4. IO for Each Surface

An ordinary Zhen processor has the following by default:

- Item slot 0: input.
- Item slot 1: output.
- All six directions: expose both item input and output regions.
- Dew processors: additionally have a 1000 mB fluid output tank.

When the Zhen Bus exposes a capability externally, it first determines the surface processor corresponding to the queried direction, then intersects the permitted regions with the input/output slots. An automation device connected to one side accesses only that side's processor and does not automatically access the other five surfaces.

## 5. Bus Automation Steps

1. Assign one function to each direction and record the surface orientation.
2. Install each surface using an actual Zhen block. After installation, verify the type with Jade or a small one-cycle recipe.
3. Connect the input pipeline to that surface. By default, the same surface also permits output extraction.
4. Connect a fluid extraction device to a Dew surface to prevent its internal 1000 mB tank from filling up.
5. Add an entity collection solution for recipes with `drop_output`.
6. Finally, perform a multi-surface parallel stress test to confirm that materials are not routed to the wrong surfaces.

## 6. Jade Information

When Jade is installed, aiming at an installed surface can replace the block name with the corresponding Zhen name and display item, fluid, and energy information. Energy text uses `current value / capacity FE`.

Jade is an optional dependency. Its absence does not stop processing, but players lose intuitive diagnostic information. The current Chinese language file contains names for the Zhen Bus and its item and fluid plugins.

## 7. Limitations and Troubleshooting

- If a pipe cannot connect at all, first confirm that it is connected to a surface with an installed processor rather than an empty surface.
- If input enters the wrong function, recheck the physical direction and ray-selected surface. Do not rely only on rendered color.
- If processing completes but random products are not visible, check the world-drop direction and entity collection range.
- The bus has no unified shared inventory. Storage on its six surfaces is independent, so do not rely on automatic internal transfer within the bus.
- The current source code has no conventional GUI configuration workflow for surface processors. Complex diagnosis relies on Jade, external transfer devices, or development debugging.
