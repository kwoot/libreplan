/*
 * This file is part of LibrePlan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Thin Chart.js wrapper driving the "load"/"earned value" charts, replacing the old
 * ZK Timeplot widget. The server (org.libreplan.web.planner.chart.ChartFiller) fully computes
 * each series (already interpolated to one point per visible day/week) and pushes it here as a
 * plain JS object via Clients.evalJavaScript - there's no live/streaming data, every call is a
 * full re-render triggered by a zoom or checkbox change already round-tripped through the server.
 */
window.LibreplanChart = (function() {

    var charts = {};

    function toDataset(series) {
        var hasFill = !!series.fillColor;
        var hasLine = series.lineWidth > 0;
        return {
            label: series.label || "",
            data: series.data,
            borderColor: hasLine ? series.lineColor : "transparent",
            borderWidth: hasLine ? series.lineWidth : 0,
            backgroundColor: hasFill ? series.fillColor : "transparent",
            fill: hasFill,
            pointRadius: 0,
            tension: 0
        };
    }

    function render(divId, config) {
        var el = document.getElementById(divId);
        if (!el) {
            return;
        }

        if (charts[divId]) {
            charts[divId].destroy();
            delete charts[divId];
        }

        // Chart.js with responsive:false keeps whatever width/height the <canvas> element
        // starts with - it does NOT read the parent div's CSS size. Without explicit width/
        // height attributes a bare <canvas> defaults to the browser's native 300x150, which is
        // why the chart used to render small in the top-left corner regardless of how big this
        // div actually was. Size the canvas itself, not just its wrapper.
        var width = config.width || el.clientWidth || 300;
        var height = config.height || 150;

        el.style.width = width + "px";
        el.style.height = height + "px";

        var canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        canvas.style.width = width + "px";
        canvas.style.height = height + "px";
        el.innerHTML = "";
        el.appendChild(canvas);

        charts[divId] = new Chart(canvas.getContext("2d"), {
            type: "line",
            data: {
                labels: config.labels,
                datasets: (config.series || []).map(toDataset)
            },
            options: {
                responsive: false,
                maintainAspectRatio: false,
                animation: false,
                interaction: {
                    mode: "index",
                    intersect: false
                },
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    x: {
                        grid: {
                            display: false
                        }
                    },
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    return {
        render: render
    };
})();
