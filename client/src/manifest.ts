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
                    name: ["40-4"],
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
                    name: "greenCircle",
                    srcs: "../other/greenCircle.png"
                }
            ]
        }
    ]
}