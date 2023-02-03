import {ResolverManifest} from "@pixi/assets";

export const manifest: ResolverManifest = {
    bundles: [
        {
            name: "ui",
            assets: [
                {
                    name: "Sourcehanserifcn Vf.ttf",
                    srcs: "/ui/SourceHanSerifCN-VF.ttf.woff2"
                },
                {
                    name: "Sourcehansanscn Vf.ttf",
                    srcs: "/ui/SourceHanSansCN-VF.ttf.woff2"
                }
            ]
        },
        {
            name: "blocks",
            assets: [
                {
                    name: ["1", "unknown"],
                    srcs: "blocks/hexagon/gray.png"
                }
            ]
        }
    ]
}