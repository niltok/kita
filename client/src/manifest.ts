import {ResolverManifest} from "@pixi/assets";

export const manifest: ResolverManifest = {
    bundles: [
        {
            name: "ui",
            assets: [
                {
                    name: "Sourcehanserifcn Vf.ttf",
                    srcs: "../ui/SourceHanSerifCN-VF.ttf.woff2"
                },
                {
                    name: "Sourcehansanscn Vf.ttf",
                    srcs: "../ui/SourceHanSansCN-VF.ttf.woff2"
                },
                {
                    name: "greenCircle",
                    srcs: "../ui/greenCircle.png"
                },
                {
                    name: "greenArrow",
                    srcs: "../ui/greenArrow.png"
                },
                {
                    name: "redArrow",
                    srcs: "../ui/redArrow.png"
                }
            ]
        },
        {
            name: "blocks",
            assets: [
                {
                    name: ["0-0", "unknown"],
                    srcs: "../blocks/hexagon/blue.png"
                },
                {
                    name: ["1-0"],
                    srcs: "../blocks/hexagon/brown.png"
                },
                {
                    name: ["1-1"],
                    srcs: "../blocks/hexagon/green.png"
                },
                {
                    name: ["2-0"],
                    srcs: "../blocks/hexagon/gray.png"
                },
                {
                    name: ["3-0"],
                    srcs: "../blocks/hexagon/violet.png"
                },
                {
                    name: ["4-0"],
                    srcs: "../blocks/hexagon/orange.png"
                },
                {
                    name: ["5-0"],
                    srcs: "../blocks/hexagon/cyan.png"
                },
                {
                    name: ["40-4"],
                    srcs: "../blocks/hexagon/red.png"
                }
            ]
        },
        {
            name: "bullet",
            assets: [
                {
                    name: ["defaultWeapon"],
                    srcs: "../bullets/Primogem.png"
                },
                {
                    name: ["r400"],
                    srcs: "../blocks/hexagon/red.png"
                }
            ]
        },
        {
            name: "other",
            assets: [
                {
                    name: "paimon",
                    srcs: "../other/paimon.png"
                },
                {
                    name: "ring",
                    srcs: "../other/ring.png"
                }
            ]
        }
    ]
}